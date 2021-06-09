/*
 * Copyright (c) 2020, Xiaomi and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaomi.duckling.engine

import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.Document
import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.engine.LexiconLookup.{lookupLexicon, lookupLexiconAnywhere}
import com.xiaomi.duckling.engine.MultiCharLookup.{lookupMultiChar, lookupMultiCharAnywhere}
import com.xiaomi.duckling.engine.PhraseLookup._
import com.xiaomi.duckling.engine.RegexLookup._
import com.xiaomi.duckling.engine.VarcharLookup._
import com.xiaomi.duckling.types.{LanguageInfo, Node}

object Engine extends LazyLogging {

  private val verbose = conf.getConfig("engine.verbose")
  private val verboseParse = verbose.getBoolean("parse")
  private val verboseMatch = verbose.getBoolean("match")
  private val verboseProduce = verbose.getBoolean("produce")
  private val verboseLookup = verbose.getBoolean("lookup")

  /**
    * A match is full if its rule pattern is empty.
    * (rule, endPosition, reversedRoute)
    */
  type Match = (Rule, Int, List[Node])

  def parse(rules: List[Rule],
            lang: LanguageInfo,
            options: Options): List[Node] = {
    val input = lang.sentence
    val doc = Document.fromLang(lang)
    val stash = parseString(rules, doc, options)
    val orderedList = stash.toPosOrderedList()
    val full =
      if (options.full) orderedList.filter(r => r.range.rangeEq(0, input.length)) else orderedList
    full.distinct
  }

  def parseAndResolve(rules: List[Rule],
                      doc: Document,
                      context: Context,
                      options: Options): List[ResolvedToken] = {
    val input = doc.rawInput
    val stash = parseString(rules, doc, options)
    val orderedList = stash.toPosOrderedList()
    val rs = orderedList.flatMap(resolveNode(doc, context, options))
    val full =
      if (options.full) rs.filter(r => r.range.rangeEq(0, input.length)) else rs
    full.distinct
  }

  def parseString(rules: List[Rule], doc: Document, options: Options): Stash = {
    // One the first pass we try all the rules
    val (new_, partialMatches) =
      parseString1(rules, doc, Stash.empty(), Stash.empty(), Nil, options)
    // For subsequent passes, we only try rules starting with a predicate.
    if (new_.isEmpty) Stash.empty()
    else {
      val headPredicateRules = rules.filter {
        case Rule(_, ItemPredicate(_) :: _, _, _) => true
        case _                                    => false
      }
      saturateParseString(headPredicateRules, doc, new_, new_, partialMatches, options)
    }
  }

  /**
    * Finds new matches resulting from newly added tokens.
    * Produces new tokens from full matches.
    *
    * @param rules
    * @param doc
    * @param stash
    * @param new_
    * @param matches
    * @return
    */
  def parseString1(rules: List[Rule],
                   doc: Document,
                   stash: Stash,
                   new_ : Stash,
                   matches: List[Match],
                   options: Options): (Stash, List[Match]) = {
    // Recursively match patterns.
    // Find which `matches` can advance because of `new`.
    val newPartial = matches.flatMap(matchFirst(doc, new_))

    // Find new matches resulting from newly added tokens (`new`)
    val newMatches = rules.flatMap(matchFirstAnywhere(doc, new_))

    val (full, partial) =
      matchAll(doc, stash, newPartial ++ newMatches).partition {
        case (Rule(_, pattern, _, _), _, _) => pattern.isEmpty
      }
    if (verboseParse) {
      if (full.isEmpty) logger.info("full: []")
      else {
        logger.info("full: [")
        full.foreach(m => logger.info(s" - $m"))
        logger.info("full: ]")
      }

      if (partial.isEmpty) logger.info("full: []")
      else {
        logger.info("partial: [")
        partial.foreach(m => logger.info(s" - $m"))
        logger.info("partial: ]")
      }
    }
    // 观察到在极端case下 .toSet.toList 比 .distinct 更快
    (Stash.fromList(full.flatMap(produce(options)).toSet.toList), partial ++ matches)
  }

  /**
    * Produces all tokens recursively.
    *
    * @param rules
    * @param sentence
    * @param stash
    * @param new_
    */
  def saturateParseString(rules: List[Rule],
                          sentence: Document,
                          stash: Stash,
                          new_ : Stash,
                          matches: List[Match],
                          options: Options): Stash = {
    val (new__, matches_) = parseString1(rules, sentence, stash, new_, matches, options)
    val stash_ = stash.union(new__)
    if (new__.isEmpty) stash
    else saturateParseString(rules, sentence, stash_, new__, matches_, options)
  }

  def resolveNode(doc: Document, context: Context, options: Options)(
    node: Node
  ): Option[ResolvedToken] = {
    val unode @ Node(r, Token(dim, data), _, _, _, _) =
      if (options.varcharExpand) endsVarcharExpansion(doc, node, options) else node
    if (unode.isValid(doc)) {
      data.resolve(context, options).map {
        case (value, latent) =>
          ResolvedToken(range = r, node = unode, value = value, isLatent = latent)
      }
    } else None
  }

  /**
    * Returns all matches matching the first pattern item of `match`, resuming from a Match position
    *
    * @param sentence
    * @param stash
    * @param `match`
    * @return
    */
  def matchFirst(sentence: Document, stash: Stash)(`match`: Match): List[Match] = {
    val (rule, position, route) = `match`
    if (rule.pattern.isEmpty) Nil
    else {
      val p :: ps = rule.pattern
      val newRule = rule.copy(pattern = ps)
      if (verboseMatch) {
        logger.info(s"apply rule of: ${rule.name}")
      }
      val valid = lookupItem(sentence, p, stash, position)
      valid.map(mkMatch(route, newRule))
    }
  }

  /**
    * Returns all matches matching the first pattern item of `match`,
    * starting anywhere
    *
    * @param sentence
    * @param stash
    * @param rule
    * @return
    */
  def matchFirstAnywhere(sentence: Document, stash: Stash)(rule: Rule): List[Match] = {
    if (rule.pattern.isEmpty) Nil
    else {
      val p :: ps = rule.pattern
      lookupItemAnywhere(sentence, p, stash).map(mkMatch(Nil, rule.copy(pattern = ps)))
    }
  }

  def lookupItemAnywhere(doc: Document, patternItem: PatternItem, stash: Stash): List[Node] = {
    patternItem match {
      case ItemRegex(re) => lookupRegexAnywhere(doc, re)
      case ItemPredicate(p) =>
        stash.toPosOrderedList().filter(node => (p orElse emptyPredicate)(node.token))
      case ItemVarchar(lower, upper, excludes) => lookupVar(doc, lower, upper, 0, excludes)
      case ItemPhrase(fn, min, max)            => lookupPhraseAnywhere(doc, 0, fn, min, max)
      case ItemMultiChar                       => lookupMultiCharAnywhere(doc, 0)
      case ItemLexicon(dict)                  => lookupLexiconAnywhere(doc, 0, dict)
    }
  }

  def produce(options: Options)(`match`: Match): Option[Node] = `match` match {
    case (Rule(name, _, _, _), _, Nil) =>
      if (verboseProduce) logger.info(s"rule: $name, reverse route: []")
      None
    case (
        Rule(name, _, production, extraction),
        _,
        etuor @ Node(Range(_, e), _, _, _, _, _) :: _
        ) =>
      val route = etuor.reverse
      val maybeToken = production.orElse(emptyProduction).apply((options, route.map(_.token)))

      if (verboseProduce) {
        logger.info(s"rule: $name, nodes: \n${route.map(n => s"  -- $n").mkString("\n")}")
        logger.info(s"prod: ${maybeToken match {
          case None        => "nothing"
          case Some(token) => token.toString
        }}")
      }

      route match {
        case Node(Range(p, _), _, _, _, _, _) :: _ if maybeToken.nonEmpty =>
          Some(
            Node(
              range = Range(p, e),
              token = maybeToken.get,
              children = route,
              rule = Some(name),
              production = production,
              features = extraction
            )
          )
        case _ => None
      }
  }

  def mkMatch(route: List[Node], newRule: Rule)(node: Node): Match = {
    val newRoute = node :: route
    (newRule, node.range.end, newRoute)
  }

  /**
    * Recursively augments `matches`.
    * Discards partial matches stuck by a regex.
    *
    * @param doc
    * @param stash
    * @param matches
    * @return
    */
  def matchAll(doc: Document, stash: Stash, matches: List[Match]): List[Match] = {
    def mkNextMatches(`match`: Match): List[Match] = {
      `match` match {
        case (Rule(_, Nil, _, _), _, _) => List(`match`)
        case (Rule(_, p :: _, _, _), _, _) =>
          val firstMatches = matchFirst(doc, stash)(`match`)
          val nextMatches = matchAll(doc, stash, firstMatches)
          p match {
            case _: ItemPredicate => `match` :: nextMatches
            case _                => nextMatches
          }
      }
    }

    matches.flatMap(mkNextMatches).flatMap {
      case (rule, n, nodes) =>
        val validNodes = nodes.filter(_.isValid(doc))
        if (validNodes.isEmpty) None
        else Some(rule, n, validNodes.toSet.toList)
    }
  }

  // lookupItem :: Document -> PatternItem -> Stash -> Int -> Duckling [Node]
  def lookupItem(doc: Document,
                 patternItem: PatternItem,
                 stash: Stash,
                 position: Int): List[Node] = {
    patternItem match {
      case ItemRegex(re) =>
        lookupRegex(doc, re, position).filter(doc.isPositionValid(position))
      case ItemPredicate(p) =>
        val after = stash.toPosOrderedListFrom(position)
        val valid = after.takeWhile(doc.isPositionValid(position))
        val left = valid.filter(n => (p orElse emptyPredicate)(n.token))
        if (verboseLookup) {
          logger.info(s"lookup: after $position => \n${after.map(n => s" -- $n").mkString("\n")}")
          logger.info(s"lookup: position valid => \n${valid.map(n => s" -- $n").mkString("\n")}")
          logger.info(s"lookup: predicate valid => \n${left.map(n => s" -- $n").mkString("\n")}")
        }
        left
      case ItemVarchar(lower, upper, excludes) =>
        lookupVarLength(doc, lower, upper, position, excludes).filter(doc.isPositionValid(position))
      case ItemPhrase(fn, min, max) => lookupPhrase(doc, position, fn, min, max)
      case ItemMultiChar            => lookupMultiChar(doc, position)
      case ItemLexicon(dict)       => lookupLexicon(doc, position, dict)
    }
  }
}

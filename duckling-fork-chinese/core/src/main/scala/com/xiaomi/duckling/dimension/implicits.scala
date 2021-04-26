package com.xiaomi.duckling.dimension

import scala.util.matching.Regex

import com.xiaomi.duckling.Types.{ItemLexicon, ItemPhrase, ItemPredicate, ItemRegex, ItemVarchar, PatternItem, Predicate, Range, Token}
import com.xiaomi.duckling.engine.LexiconLookup.Dict
import com.xiaomi.duckling.engine.PhraseLookup.PhraseMatcherFn
import com.xiaomi.duckling.ranking.Types.Feature

object implicits {

  implicit def ordering: Ordering[Range] = Ordering.by(o => (o.start, o.end))

  implicit class ItemLexiconWrapper(dict: Dict) {
    def lexicon: ItemLexicon = ItemLexicon(dict)
  }

  case object ItemMultiChar extends PatternItem {
    override def predicate(token: Token): Boolean = true
  }

  implicit class RegexPredicateWrapper(s: String) {
    def regex: ItemRegex = ItemRegex(s.r)
  }

  implicit class ItemPredicateWrapper(p: Predicate) {
    def predicate: ItemPredicate = ItemPredicate(p)
  }

  implicit class VarLengthPredicateWrapper(bound: (Int, Int)) {
    def varchar: ItemVarchar = ItemVarchar(bound._1, bound._2)
  }

  implicit class VarLengthPredicateCustomWrapper(bound: (Int, Int, List[Regex])) {
    def varchar: ItemVarchar = ItemVarchar.tupled(bound)
  }

  implicit class PhrasePredicateWrapper(tuple: (PhraseMatcherFn, Int, Int)) {
    def phrase: ItemPhrase = ItemPhrase.tupled(tuple)
  }

  implicit def toOption[T](t: T) = Some(t)

  implicit def toList(t: Feature): List[Feature] = t :: Nil
}

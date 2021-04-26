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

package com.xiaomi.duckling

import com.xiaomi.duckling.Types.{Context, Options, ResolvedToken}
import com.xiaomi.duckling.dimension.place.{Place, PlaceData}

object PlaceQuery {

  def noPunc(s: String): String = {
    s.replace(160.toChar, 32.toChar).replaceAll("[\\pP‘’“”]", "")
  }

  def extract(s: String): Option[String] = {
    val ns = noPunc(s)
    val answers = Api.analyze(ns, new Context(), Options(targets = Set(Place)))
    val candidates = answers.flatMap { answer =>
      answer.token.value match {
        case placeData: PlaceData =>
          placeData.candidates.map(c => (c.getPathStr(), answer.token))
        case _ => Nil
      }
    }
    if (candidates.nonEmpty) {
      // 1. 遇到非中国，直接返回国家，丢掉其它所有结果
      val foreign = foreignPlace(candidates.head._2)
      if (foreign.nonEmpty) Some(candidates.head._1)
      else {
        // 2. 相同解析的，只留下匹配度最高的
        val d1 = candidates.groupBy(_._1).mapValues(_.maxBy(_._2.range.length)).values
        // 3. 相同范围不同解析的，留下最路径短的，湖南省长沙市/湖南省长沙市长沙县
        val d2 = d1.groupBy(_._2.range).mapValues(_.minBy(_._1.split("/").length)).values
        // 4. 取匹配度最高的一个结果
        Some(d2.maxBy(_._2.range.length)._1)
      }
    } else None
  }

  def foreignPlace(token: ResolvedToken): Option[String] = {
    val first = token.value.asInstanceOf[PlaceData].candidates.headOption
    if (first.exists(c => c.category == "国家" && c.name != "中华人民共和国")) {
      first.map(_.getPathStr())
    } else None
  }
}

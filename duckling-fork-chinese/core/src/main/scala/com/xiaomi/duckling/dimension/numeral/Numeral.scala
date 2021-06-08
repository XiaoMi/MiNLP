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

package com.xiaomi.duckling.dimension.numeral

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.Dimension
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.numeral.seq.DigitSequence
import com.xiaomi.duckling.dimension.time.enums.{IntervalDirection, IntervalType}

case object Numeral extends Dimension with Rules with Examples {
  override val name: String = "Numeral"

  override val dimDependents: List[Dimension] = List(DigitSequence)
}

case class NumeralData(value: Double,
                       grain: Option[Int] = None,
                       multipliable: Boolean = false,
                       sequence: Option[String] = None,
                       composable: Boolean = true,
                       precision: Int = 0)
    extends Resolvable {
  override def resolve(context: Context, options: Options): Option[(ResolvedValue, Boolean)] = {
    (NumeralValue(value, precision), false)
  }

  def isCnSeq: Boolean = sequence.exists(s => !s.headOption.exists(c => c >= '0' && c <= '9'))

  override def toString: String = {
    val sv = s"v = $value"
    val sg = grain match {
      case Some(g) => s", grain = $g"
      case None => ""
    }
    val sm = s", multipliable = $multipliable"
    s"{$sv$sg$sm}"
  }
}

/**
  *
  * @param allowZeroLeadingDigits 允许000318解析为318
  * @param cnSequenceAsNumber 允许一二三四解析出1234
  */
case class NumeralOptions(allowZeroLeadingDigits: Boolean = false,
                          cnSequenceAsNumber: Boolean = false)

trait IntervalValue extends ResolvedValue

case class NumeralValue(n: Double, precision: Int = 0) extends IntervalValue {
  override def schema(): Option[String] = Some(s"$n")
}

/**
  * 区间，默认左闭右开
  */
case class DoubleSideIntervalValue(left: Double,
                                   right: Double,
                                   leftType: IntervalType = IntervalType.Closed,
                                   rightType: IntervalType = IntervalType.Open)
    extends IntervalValue {
  override def schema(): Option[String] = Some(s"$left<$right")
}

/**
  * 开区间
  */
case class OpenIntervalValue(start: Double, direction: IntervalDirection) extends IntervalValue {
  override def schema(): Option[String] = {
    if (direction == IntervalDirection.After) Some(s">${start}")
    else Some(s"<${start}")
  }
}

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

package com.xiaomi.duckling.ranking

object Types {
  type BagOfFeatures = Map[String, Int]
  type Class = Boolean
  type Datum = (BagOfFeatures, Class)

  trait Feature {
    def name(): String

    def value(): Double
  }

  case class DiscreteFeature(s: String, count: Int = 1) extends Feature {
    override def name(): String = {
      if (count == 1) s
      else "%s_%d".format(s, count)
    }

    override def value(): Double = 1.0
  }

  case class RealFeature(name: String, value: Double) extends Feature
}

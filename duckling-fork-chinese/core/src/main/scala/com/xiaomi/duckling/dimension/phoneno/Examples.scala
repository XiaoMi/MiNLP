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

package com.xiaomi.duckling.dimension.phoneno

import com.xiaomi.duckling.dimension.DimExamples
import com.xiaomi.duckling.dimension.implicits._

trait Examples extends DimExamples {
  override val pairs = List(
    (PhoneNumberData("6507018887"), List("650-701-8887")),
    (
      PhoneNumberData("6507018887", "1"),
      List("(+1)650-701-8887", "(+1)   650 - 701  8887", "(+1) 650-701-8887", "+1 6507018887")
    ),
    (PhoneNumberData("146647998", "33"), List("+33 1 46647998")),
    (PhoneNumberData("0620702220"), List("06 2070 2220")),
    (PhoneNumberData("6507018887", ext = "897"), List("(650)-701-8887 ext 897")),
    (PhoneNumberData("2025550121", "1"), List("+1-202-555-0121", "+1 202.555.0121")),
    (PhoneNumberData("4866827"), List("4.8.6.6.8.2.7")),
    (PhoneNumberData("06354640807"), List("06354640807")),
    (PhoneNumberData("18998078030"), List("18998078030")),
    (PhoneNumberData("61992852776"), List("61 - 9 9285-2776")),
    (PhoneNumberData("19997424919"), List("(19) 997424919")),
    (PhoneNumberData("19992842606", "55"), List("+55 19992842606")),
    (PhoneNumberData("18611223344", "86"), List("+8618611223344")),
    (PhoneNumberData("01086543210"), List("010-86543210")),
    (PhoneNumberData("01086543210", "86"), List("+86 010-86543210"))
  )
}

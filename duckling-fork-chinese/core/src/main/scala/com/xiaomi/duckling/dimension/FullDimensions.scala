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

package com.xiaomi.duckling.dimension

import com.xiaomi.duckling.dimension.act.Act
import com.xiaomi.duckling.dimension.age.Age
import com.xiaomi.duckling.dimension.bloodtype.BloodType
import com.xiaomi.duckling.dimension.constellation.Constellation
import com.xiaomi.duckling.dimension.currency.Currency
import com.xiaomi.duckling.dimension.duplicate.Duplicate
import com.xiaomi.duckling.dimension.episode.Episode
import com.xiaomi.duckling.dimension.gender.Gender
import com.xiaomi.duckling.dimension.level.Level
import com.xiaomi.duckling.dimension.multichar.MultiChar
import com.xiaomi.duckling.dimension.numeral.multiple.Multiple
import com.xiaomi.duckling.dimension.music.Lyric
import com.xiaomi.duckling.dimension.numeral.Numeral
import com.xiaomi.duckling.dimension.numeral.fraction.Fraction
import com.xiaomi.duckling.dimension.numeral.seq.DigitSequence
import com.xiaomi.duckling.dimension.ordinal.Ordinal
import com.xiaomi.duckling.dimension.phoneno.PhoneNumber
import com.xiaomi.duckling.dimension.place.Place
import com.xiaomi.duckling.dimension.quantity.Quantity
import com.xiaomi.duckling.dimension.quantity.area.Area
import com.xiaomi.duckling.dimension.quantity.distance.Distance
import com.xiaomi.duckling.dimension.quantity.velocity.Velocity
import com.xiaomi.duckling.dimension.rating.Rating
import com.xiaomi.duckling.dimension.season.Season
import com.xiaomi.duckling.dimension.temperature.Temperature
import com.xiaomi.duckling.dimension.time.date.Date
import com.xiaomi.duckling.dimension.time.Time
import com.xiaomi.duckling.dimension.time.duration.Duration
import com.xiaomi.duckling.dimension.time.repeat.Repeat
import com.xiaomi.duckling.dimension.url.DuckURL

class FullDimensions extends Dimensions {
  override val dims: List[Dimension] = List(
    Act,
    Age,
    Area,
    BloodType,
    Constellation,
    Currency,
    Date,
    DigitSequence,
    Distance,
    DuckURL,
    Duplicate,
    Duration,
    Episode,
    Fraction,
    Gender,
    Level,
    Lyric,
    MultiChar,
    Multiple,
    Numeral,
    Ordinal,
    PhoneNumber,
    Place,
    Quantity,
    Rating,
    Repeat,
    Season,
    Temperature,
    Time,
    Velocity
  )
}

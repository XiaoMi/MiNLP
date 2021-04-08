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

package duckling.dimension

import duckling.dimension.age.Age
import duckling.dimension.bloodtype.BloodType
import duckling.dimension.constellation.Constellation
import duckling.dimension.currency.Currency
import duckling.dimension.duplicate.Duplicate
import duckling.dimension.episode.Episode
import duckling.dimension.gender.Gender
import duckling.dimension.level.Level
import duckling.dimension.multichar.MultiChar
import duckling.dimension.numeral.multiple.Multiple
import duckling.dimension.music.Lyric
import duckling.dimension.numeral.Numeral
import duckling.dimension.numeral.fraction.Fraction
import duckling.dimension.numeral.seq.DigitSequence
import duckling.dimension.ordinal.Ordinal
import duckling.dimension.phoneno.PhoneNumber
import duckling.dimension.place.Place
import duckling.dimension.quantity.Quantity
import duckling.dimension.quantity.velocity.Velocity
import duckling.dimension.rating.Rating
import duckling.dimension.season.Season
import duckling.dimension.temperature.Temperature
import duckling.dimension.time.date.Date
import duckling.dimension.time.Time
import duckling.dimension.time.duration.Duration
import duckling.dimension.url.DuckURL

class FullDimensions extends Dimensions {
  override val dims: List[Dimension] = List(
    Numeral,
    Ordinal,
    Date,
    Time,
    Lyric,
    Constellation,
    Place,
    Gender,
    Place,
    DigitSequence,
    Quantity,
    Duration,
    Currency,
    Fraction,
    Level,
    Age,
    Rating,
    Temperature,
    Velocity,
    Season,
    Episode,
    BloodType,
    DuckURL,
    PhoneNumber,
    MultiChar,
    Duplicate,
    Multiple
  )
}

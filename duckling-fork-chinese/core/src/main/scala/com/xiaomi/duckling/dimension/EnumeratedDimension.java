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

package com.xiaomi.duckling.dimension;


public enum EnumeratedDimension {
    Age(com.xiaomi.duckling.dimension.age.Age$.MODULE$),
    Area(com.xiaomi.duckling.dimension.quantity.area.Area$.MODULE$),
    BloodType(com.xiaomi.duckling.dimension.bloodtype.BloodType$.MODULE$),
    Constellation(com.xiaomi.duckling.dimension.constellation.Constellation$.MODULE$),
    Currency(com.xiaomi.duckling.dimension.currency.Currency$.MODULE$),
    Date(com.xiaomi.duckling.dimension.time.date.Date$.MODULE$),
    DigitSequence(com.xiaomi.duckling.dimension.numeral.seq.DigitSequence$.MODULE$),
    Distance(com.xiaomi.duckling.dimension.quantity.distance.Distance$.MODULE$),
    Duplicate(com.xiaomi.duckling.dimension.duplicate.Duplicate$.MODULE$),
    Duration(com.xiaomi.duckling.dimension.time.duration.Duration$.MODULE$),
    Episode(com.xiaomi.duckling.dimension.episode.Episode$.MODULE$),
    Fraction(com.xiaomi.duckling.dimension.numeral.fraction.Fraction$.MODULE$),
    Gender(com.xiaomi.duckling.dimension.gender.Gender$.MODULE$),
    Level(com.xiaomi.duckling.dimension.level.Level$.MODULE$),
    Lyric(com.xiaomi.duckling.dimension.music.Lyric$.MODULE$),
    MultiChar(com.xiaomi.duckling.dimension.multichar.MultiChar$.MODULE$),
    Multiple(com.xiaomi.duckling.dimension.numeral.multiple.Multiple$.MODULE$),
    Numeral(com.xiaomi.duckling.dimension.numeral.Numeral$.MODULE$),
    Ordinal(com.xiaomi.duckling.dimension.ordinal.Ordinal$.MODULE$),
    PhoneNumber(com.xiaomi.duckling.dimension.phoneno.PhoneNumber$.MODULE$),
    Place(com.xiaomi.duckling.dimension.place.Place$.MODULE$),
    Quantity(com.xiaomi.duckling.dimension.quantity.Quantity$.MODULE$),
    Rating(com.xiaomi.duckling.dimension.rating.Rating$.MODULE$),
    Repeat(com.xiaomi.duckling.dimension.time.repeat.Repeat$.MODULE$),
    Season(com.xiaomi.duckling.dimension.season.Season$.MODULE$),
    Temperature(com.xiaomi.duckling.dimension.temperature.Temperature$.MODULE$),
    Time(com.xiaomi.duckling.dimension.time.Time$.MODULE$),
    URL(com.xiaomi.duckling.dimension.url.DuckURL$.MODULE$),
    Velocity(com.xiaomi.duckling.dimension.quantity.velocity.Velocity$.MODULE$);

    private Dimension dimension;

    EnumeratedDimension(Dimension dimension) {
        this.dimension = dimension;
    }

    public Dimension getDimension() {
        return dimension;
    }
}

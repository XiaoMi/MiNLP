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

package duckling.dimension;


public enum EnumeratedDimension {
    Age(duckling.dimension.age.Age$.MODULE$),
    BloodType(duckling.dimension.bloodtype.BloodType$.MODULE$),
    Constellation(duckling.dimension.constellation.Constellation$.MODULE$),
    Currency(duckling.dimension.currency.Currency$.MODULE$),
    Episode(duckling.dimension.episode.Episode$.MODULE$),
    Gender(duckling.dimension.gender.Gender$.MODULE$),
    Level(duckling.dimension.level.Level$.MODULE$),
    Lyric(duckling.dimension.music.Lyric$.MODULE$),
    Numeral(duckling.dimension.numeral.Numeral$.MODULE$),
    Fraction(duckling.dimension.numeral.fraction.Fraction$.MODULE$),
    DigitSequence(duckling.dimension.numeral.seq.DigitSequence$.MODULE$),
    Ordinal(duckling.dimension.ordinal.Ordinal$.MODULE$),
    Place(duckling.dimension.place.Place$.MODULE$),
    Quantity(duckling.dimension.quantity.Quantity$.MODULE$),
    Velocity(duckling.dimension.quantity.velocity.Velocity$.MODULE$),
    Rating(duckling.dimension.rating.Rating$.MODULE$),
    Season(duckling.dimension.season.Season$.MODULE$),
    Temperature(duckling.dimension.temperature.Temperature$.MODULE$),
    Time(duckling.dimension.time.Time$.MODULE$),
    Date(duckling.dimension.time.date.Date$.MODULE$),
    Duration(duckling.dimension.time.duration.Duration$.MODULE$),
    URL(duckling.dimension.url.DuckURL$.MODULE$),
    PhoneNumber(duckling.dimension.phoneno.PhoneNumber$.MODULE$),
    MultiChar(duckling.dimension.multichar.MultiChar$.MODULE$),
    Duplicate(duckling.dimension.duplicate.Duplicate$.MODULE$),
    Multiple(duckling.dimension.numeral.multiple.Multiple$.MODULE$);

    private Dimension dimension;

    EnumeratedDimension(Dimension dimension) {
        this.dimension = dimension;
    }

    public Dimension getDimension() {
        return dimension;
    }
}

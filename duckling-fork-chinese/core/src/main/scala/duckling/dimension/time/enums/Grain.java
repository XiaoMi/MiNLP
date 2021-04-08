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

package duckling.dimension.time.enums;

import duckling.Types.Context;
import duckling.Types.Options;
import duckling.Types.Resolvable;
import duckling.Types.ResolvedValue;
import scala.Option;
import scala.Some;
import scala.Tuple2;

/**
 * NoGrain is helpful to define "now"
 */
public enum Grain implements ResolvedValue, Resolvable {
    NoGrain, Second, Minute, Hour, Day, Week, Month, Quarter, Year;

    @Override
    public Option<Tuple2<ResolvedValue, Object>> resolve(Context context, Options options) {
        return Option.apply(Tuple2.apply(this, false));
    }

    @Override
    public Option<String> schema() {
        return Option.empty();
    }

    public Option<Grain> finer() {
        Grain g;
        switch (this) {
            case Year:
                g = Month;
                break;
            case Month:
                g = Day;
                break;
            case Day:
                g = Hour;
                break;
            case Hour:
                g = Minute;
                break;
            case Minute:
                g = Second;
                break;
            default:
                return Option.empty();
        }
        return Some.apply(g);
    }

    public static Grain resetTo(Grain grain) {
        if (grain == Grain.Quarter) {
            return Year;
        }
        return grain;
    }
}

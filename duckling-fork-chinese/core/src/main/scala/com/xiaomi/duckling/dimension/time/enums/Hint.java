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

package com.xiaomi.duckling.dimension.time.enums;

public enum Hint {
    NoHint, FinalRule, ReplacePartOfTime, YearOnly,
    /**
     * 单独的x月匹配
     */
    MonthOnly, DayOnly, YearMonth, MonthDay,
    /**
     * 由Date的规则成
     */
    Date,
    /**
     * 农历
     */
    Lunar,
    /**
     * 早/晚上位于规则右部
     */
    PartOfDayAtLast,
    /**
     * 节假日
     */
    Holiday,
    /**
     * 今明昨后
     */
    RecentNominal,
    /**
     * 最近，未来过去非明确
     */
    UncertainRecent, Recent,
    /**
     * 时间序列有先后关系类的
     */
    Sequence,
    /**
     * 需要被组合才能召回
     */
    ComposeNeeded,
    /**
     * 求交的结果
     */
    Intersect,
    /**
     * 春夏秋冬
     */
    Season
}

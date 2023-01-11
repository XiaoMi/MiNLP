package com.xiaomi.duckling.dimension.time

import com.xiaomi.duckling.Types.{Predicate, Token}
import com.xiaomi.duckling.dimension.time.enums.Grain

package object repeat {

  def isOnlyWorkdaysType: Predicate = {
    case Token(Repeat, repeat: RepeatData) => repeat.workdayType.nonEmpty && repeat.start.isEmpty
  }

  def isHourTimes: Predicate = {
    case Token(Time, td: TimeData) => td.timeGrain < Grain.Day
  }
}

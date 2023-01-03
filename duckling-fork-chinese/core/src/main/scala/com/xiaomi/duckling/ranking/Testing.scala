package com.xiaomi.duckling.ranking

import java.time.{LocalDateTime, ZonedDateTime}
import java.util.Locale

import com.xiaomi.duckling.Types.{Context, Options, ZoneCN}
import com.xiaomi.duckling.dimension.time.TimeOptions

object Testing {
  val testContext: Context =
    Context(
      locale = Locale.CHINA,
      referenceTime = ZonedDateTime.of(LocalDateTime.of(2013, 2, 12, 4, 30, 0), ZoneCN)
    )

  val testOptions: Options = Options(full = true, debug = true, timeOptions = TimeOptions(parseFourSeasons = true))
}

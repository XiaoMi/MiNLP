package com.xiaomi.duckling.server

import org.json4s.jackson.Serialization.write

import com.xiaomi.duckling.Api
import com.xiaomi.duckling.JsonSerde._
import com.xiaomi.duckling.Types.{Answer, Context, Options}

/**
 * 简化Java操作Scala对象
 */
object DuckJavaHelper {

  def answerAsJson(answers: java.util.List[Answer]): String = {
    write(answers)
  }

  def orElse[T](t: Option[T], default: T): T = {
    t.getOrElse(default)
  }
}

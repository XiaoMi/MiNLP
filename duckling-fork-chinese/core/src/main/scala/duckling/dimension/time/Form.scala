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

package duckling.dimension.time

object form {

  sealed trait Form

  case object DayOfWeek extends Form

  case class TimeOfDay(hours: Option[Int], is12H: Boolean) extends Form

  case object IntervalOfDay extends Form

  case class Month(month: Int) extends Form

  case class PartOfDay(part: String) extends Form

}

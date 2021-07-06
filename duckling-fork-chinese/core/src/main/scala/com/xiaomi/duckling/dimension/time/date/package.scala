package com.xiaomi.duckling.dimension.time

import com.xiaomi.duckling.dimension.time.enums.Grain
import com.xiaomi.duckling.dimension.time.enums.Grain._

package object date {

	def weekOffsetSchema(offset: Int, dayOfWeek: Int): Option[String] = {
		Some("W_" + offset + "-" + dayOfWeek)
	}

	def offsetSchema(grain: Grain, offset: Int): Option[String] = {
		grain match {
			case Second => Some("[EXT][DATE]TS_" + offset)
			case Minute => Some("[EXT][DATE]TM_" + offset)
			case Hour => Some("[EXT][DATE]TH_" + offset)
			case Day => Some("[EXT][DATE]D_" + offset)
			case Week => Some("[EXT][DATE]W_" + offset)
			case Month => Some("[EXT][DATE]M_" + offset)
			case Quarter => Some("[EXT][DATE]Q_" + offset)
			case Year => Some("[EXT][DATE]Y_" + offset)
			case _ => None
		}
	}

	/**
	  * 日期schema，年月日, yyyy-MM-DD
	  * @param year   年
	  * @param month  月
	  * @param day    日
	  * @return
	  */
	def dateSchema(year: Option[Int] = None,
				   month: Option[Int] = None,
				   day: Option[Int] = None): Option[String] = {
		var schema: String = if (year.isDefined) year.get.toString else ""

		if (month.isDefined) {
			val mm = if(month.get > 9) month.get.toString else "0" + month.get.toString
			schema = if (schema.length > 0) schema + '-' + mm else mm
		}

		if (day.isDefined) {
			val dd = if(day.get > 9) day.get.toString else "0" + day.get.toString
			schema = if (schema.length > 0) schema + '-' + dd else dd
		}

		Some(schema)
	}

	/**
	  * 日期schema, yyyJ
	  * @param decade 年代
	  * @return       IS
	  */
	def decadeSchema(decade: Int): Option[String] = {
		val decadeItem  = decade.toString

		if (decadeItem.endsWith("0") && decadeItem.length == 4) {
			Some(decadeItem.substring(0, 3) + "J")
		} else {
			None
		}
	}

	/**
	  * 特殊日期schema
	  * 最近
	  * @return       IS
	  */
	def recentSchema(): Option[String] = {
		Some(TimeConstant.RECENT)
	}
}
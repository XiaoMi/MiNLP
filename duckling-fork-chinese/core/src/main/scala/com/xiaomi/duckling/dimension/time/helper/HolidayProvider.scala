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

package com.xiaomi.duckling.dimension.time.helper

import com.google.common.collect.{ImmutableListMultimap, ImmutableMap, Maps}
import java.time.DayOfWeek._

import scala.collection.mutable

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.time.Prods._
import com.xiaomi.duckling.dimension.time.TimeData
import com.xiaomi.duckling.dimension.time.enums.Hint
import com.xiaomi.duckling.dimension.time.helper.TimeDataHelpers._
import com.xiaomi.duckling.dimension.time.rule.LunarDays
import com.xiaomi.duckling.engine.LexiconLookup.Dict

trait HolidayProvider {

  /**
    * 到归一化值的映射
    */
  def dict: Dict

  /**
    * 节日映射
    * ptn normal token
    * @return
    */
  def holidayTokenMap: ImmutableMap[String, Token]
}

class LocalHolidayProvider extends HolidayProvider {

  val rulePeriodicHolidays: List[(Token, String, String)] = mkRuleHolidays(
    // Fixed dates, year over year
    List(
      (monthDay(1, 10), "中国人民警察节", "中国人民警察节|人民警察节|中国警察节|警察节"),
      (monthDay(2, 7), "国际声援南非日", "国际声援南非日|声援南非日"),
      (monthDay(2, 15), "中国12亿人口日", "中国12亿人口日"),
      (monthDay(2, 21), "反对殖民主义斗争日", "反对殖民主义斗争日|反对殖民制度斗争日"),
      (monthDay(2, 24), "第三世界青年日", "第三世界青年日"),
      (monthDay(3, 5), "中国青年志愿者服务日", "中国青年志愿者服务日"),
      (monthDay(3, 14), "国际警察日", "国际警察日|国际警察节"),
      (monthDay(3, 16), "手拉手情系贫困小伙伴全国统一行动日", "手拉手情系贫困小伙伴全国统一行动日"),
      (monthDay(3, 17), "国际航海日", "国际航海日|世界海事日|航海日"),
      (monthDay(3, 18), "全国科技人才活动日", "全国科技人才活动日"),
      (monthDay(3, 21), "世界儿歌日", "世界儿歌日"),
      (monthDay(3, 21), "国际消除种族歧视日", "国际消除种族歧视日"),
      (monthDay(3, 23), "世界气象日", "世界气象日|国际气象日"),
      (monthDay(4, 7), "世界健康日", "世界健康日|世界卫生日"),
      (monthDay(4, 21), "全国企业家活动日", "全国企业家活动日"),
      (monthDay(4, 22), "世界法律日", "世界法律日"),
      (monthDay(4, 22), "世界地球日", "世界地球日|地球日"),
      (monthDay(4, 24), "亚非新闻工作者日", "亚非新闻工作者日|亚非新闻工作者节"),
      (monthDay(4, 24), "世界青年反对殖民主义日", "世界青年反对殖民主义日|世界青年团结日"),
      (monthDay(4, 25), "全国儿童预防接种宣传日", "全国儿童预防接种宣传日|全国预防接种宣传日|计划免疫宣传日"),
      (monthDay(4, 26), "世界知识产权日", "世界知识产权日"),
      (monthDay(4, 27), "联谊城日", "联谊城日"),
      (monthDay(4, 30), "全国交通安全反思日", "全国交通安全反思日"),
      (monthDay(5, 4), "科技传播日", "科技传播日|五四科技传播日"),
      (monthDay(5, 5), "全国碘缺乏病防治日", "全国碘缺乏病防治日|全国碘缺乏病日"),
      (monthDay(5, 8), "世界微笑日", "世界微笑日|国际微笑日"),
      (monthDay(5, 15), "国际家庭日", "国际家庭日|家庭日"),
      (monthDay(5, 17), "世界电信日", "世界电信日"),
      (monthDay(5, 19), "中国旅游日", "中国旅游日|全国旅游日"),
      (monthDay(5, 20), "中国学生营养日", "中国学生营养日"),
      (monthDay(5, 20), "全国母乳喂养宣传日", "全国母乳喂养宣传日"),
      (monthDay(5, 26), "世界向人体条件挑战日", "世界向人体条件挑战日|向人体条件挑战世界日"),
      (monthDay(5, 30), "五卅反对帝国主义运动纪念日", "五卅反对帝国主义运动纪念日"),
      (monthDay(6, 6), "全国爱眼日", "全国爱眼日|爱眼日"),
      (monthDay(6, 17), "世界防治荒漠化与干旱日", "世界防治荒漠化与干旱日"),
      (monthDay(6, 22), "中国儿童慈善活动日", "中国儿童慈善活动日"),
      (monthDay(6, 23), "世界手球日", "世界手球日"),
      (monthDay(6, 23), "国际奥林匹克日", "国际奥林匹克日"),
      (monthDay(6, 26), "国际宪章日", "国际宪章日|联合国宪章日"),
      (monthDay(6, 30), "世界青年联欢节", "世界青年联欢节"),
      (monthDay(7, 1), "国际建筑日", "国际建筑日|世界建筑日"),
      (monthDay(7, 2), "国际体育记者日", "国际体育记者日"),
      (monthDay(7, 30), "非洲妇女日", "非洲妇女日"),
      (monthDay(8, 6), "国际电影节", "国际电影节"),
      (monthDay(8, 26), "全国律师咨询日", "全国律师咨询日"),
      (monthDay(9, 3), "中国人民抗日战争胜利纪念日", "中国人民抗日战争胜利纪念日|抗日战争胜利纪念日|抗战胜利纪念日"),
      (monthDay(9, 8), "世界扫盲日", "世界扫盲日|国际扫盲日"),
      (monthDay(9, 8), "国际新闻工作者日", "国际新闻工作者日|国际新闻工作者团结日"),
      (monthDay(1, 1), "元旦节", "元旦节|元旦|阳历新年|公历新年|新年"),
      (monthDay(1, 1), "台湾中华民国开国纪念日", "台湾中华民国开国纪念日|中华民国开国纪念日"),
      (monthDay(10, 1), "国际音乐日", "国际音乐日"),
      (monthDay(10, 1), "国际老人节", "国际老人日|国际老人节"),
      (monthDay(10, 5), "世界教师日", "世界教师日|国际教师节"),
      (monthDay(10, 1), "国庆节", "十一国庆节|国庆节|国庆日|十一|国庆"),
      (monthDay(10, 10), "台湾国庆日", "台湾国庆日"),
      (monthDay(10, 10), "辛亥革命纪念日", "辛亥革命纪念日"),
      (monthDay(10, 12), "哥伦布日", "哥伦布日"),
      (monthDay(10, 13), "世界保健日", "世界保健日"),
      (monthDay(10, 13), "世界视觉日", "世界视觉日"),
      (monthDay(10, 14), "世界标准日", "世界标准日"),
      (monthDay(10, 15), "国际盲人节", "国际盲人节|盲人节|白手杖节"),
      (monthDay(10, 16), "世界粮食日", "世界粮食日|世界食物日"),
      (monthDay(10, 17), "国际消除贫困日", "国际消除贫困日|国际灭贫日|国际消贫日"),
      (monthDay(10, 22), "世界传统医药日", "世界传统医药日"),
      (monthDay(10, 24), "联合国日", "联合国日"),
      (monthDay(10, 25), "台湾光复节", "台湾光复节"),
      (monthDay(10, 28), "世界男性健康日", "世界男性健康日"),
      (monthDay(10, 31), "万圣夜", "万圣节前夜|万圣夜"),
      (monthDay(10, 4), "世界动物日", "世界动物日"),
      (monthDay(4, 24), "中国航天日", "中国航天日"),
      (monthDay(11, 1), "万圣节", "万圣节"),
      (monthDay(11, 1), "国家宪法日", "国家宪法日"),
      (monthDay(11, 11), "光棍节", "双十一|双11|光棍节"),
      (monthDay(11, 11), "退伍军人节", "退伍军人节|老兵节"),
      (monthDay(11, 12), "国父诞辰纪念日", "国父诞辰纪念日"),
      (monthDay(11, 14), "世界糖尿病日", "世界糖尿病日"),
      (monthDay(11, 14), "世界防治糖尿病日", "世界防治糖尿病日"),
      (monthDay(11, 16), "国际宽容日", "国际宽容日"),
      (monthDay(11, 17), "世界学生日", "世界学生日|国际大学生节"),
      (monthDay(11, 20), "国际儿童日", "国际儿童日"),
      (monthDay(11, 21), "世界问候日", "世界问候日"),
      (monthDay(11, 25), "国际素食日", "国际素食节|健康素食日|健康素食节|国际素食日"),
      (monthDay(11, 5), "世界海啸日", "世界海啸日"),
      (monthDay(11, 8), "寒衣节", "寒衣节"),
      (monthDay(11, 8), "中国记者日", "中国记者日|记者日"),
      (monthDay(11, 9), "全国消防安全日", "全国消防安全日|消防安全日"),
      (monthDay(12, 1), "世界艾滋病日", "世界艾滋病日|艾滋病日"),
      (monthDay(12, 10), "人权日", "人权日"),
      (monthDay(12, 12), "双十二", "双十二|双十二电商节"),
      (monthDay(12, 12), "西安事变纪念日", "西安事变纪念日"),
      (monthDay(12, 13), "南京大屠杀纪念日", "南京大屠杀纪念日"),
      (monthDay(12, 13), "南京大屠杀死难者国家公祭日", "南京大屠杀死难者国家公祭日|国家公祭日"),
      (monthDay(12, 2), "全国交通安全日", "全国交通安全日|交通安全日"),
      (monthDay(12, 20), "澳门回归纪念日", "澳门回归纪念日|澳门回归日"),
      (monthDay(12, 24), "平安夜", "平安夜"),
      (monthDay(12, 25), "圣诞节", "圣诞节|圣诞"),
      (monthDay(12, 25), "台湾行宪纪念日", "台湾行宪纪念日|行宪纪念日"),
      (monthDay(12, 3), "残疾人日", "残疾人日"),
      (monthDay(12, 31), "新年夜", "新年夜|跨年"),
      (monthDay(2, 10), "国际气象节", "国际气象节|气象节"),
      (monthDay(2, 13), "世界无线电日", "世界无线电日|无线电日"),
      (monthDay(2, 14), "情人节", "情人节|圣瓦伦丁节"),
      (monthDay(2, 2), "世界湿地日", "世界湿地日|湿地日"),
      (monthDay(2, 21), "国际母语日", "国际母语日|母语日"),
      (monthDay(2, 28), "台湾和平纪念日", "台湾和平纪念日"),
      (monthDay(2, 28), "世界居住条件调查日", "世界居住条件调查日|居住条件调查日"),
      (monthDay(2, 4), "世界癌症日", "世界癌症日|癌症日"),
      (monthDay(3, 3), "全国爱耳日", "全国爱耳日|爱耳日"),
      (monthDay(3, 1), "国际海豹日", "国际海豹日|海豹日"),
      (monthDay(3, 12), "植树节", "中国植树节|植树节"),
      (monthDay(3, 14), "白色情人节", "白色情人节"),
      (monthDay(3, 15), "国际消费者权益日", "国际消费者权益日|世界消费者权益日|消费者权益日|三一五|消费者日"),
      (monthDay(3, 17), "中国国医节", "中国国医节|国医节"),
      (monthDay(3, 17), "圣帕特里克节", "圣帕特里克节"),
      (monthDay(3, 21), "世界森林日", "世界森林日|森林日"),
      (monthDay(3, 21), "世界睡眠日", "世界睡眠日|睡眠日"),
      (monthDay(3, 21), "世界诗歌日", "世界诗歌日|诗歌日"),
      (monthDay(3, 22), "世界水日", "世界水日"),
      (monthDay(3, 23), "国家气象日", "国家气象日|气象日"),
      (monthDay(3, 24), "世界防治结核病日", "世界防治结核病日|防治结核病日"),
      (monthDay(3, 27), "台湾青年节", "台湾青年节"),
      (monthDay(3, 27), "世界戏剧日", "世界戏剧日"),
      (monthDay(3, 5), "学雷锋纪念日", "学雷锋纪念日|雷锋纪念日|学雷锋日"),
      (monthDay(3, 7), "女生节", "女生节"),
      (monthDay(3, 8), "妇女节", "国际劳动妇女节|国际妇女节|三八妇女节|三八节|妇女节"),
      (monthDay(3, 9), "世界肾脏日", "世界肾脏日"),
      (monthDay(3, 9), "保护母亲河日", "保护母亲河日"),
      (monthDay(4, 1), "愚人节", "愚人节"),
      (monthDay(4, 11), "世界帕金森病日", "世界帕金森病日"),
      (monthDay(4, 16), "复活节", "复活节"),
      (monthDay(4, 2), "国际儿童图书日", "国际儿童图书日"),
      (monthDay(4, 2), "世界自闭症关注日", "世界提高自闭症意识日|世界自闭症日|世界自闭症关注日"),
      (monthDay(4, 23), "世界读书日", "世界读书日|读书日"),
      (monthDay(4, 26), "国际知识产权日", "国际知识产权日|知识产权日"),
      (monthDay(4, 30), "国际爵士日", "国际爵士日|爵士日"),
      (monthDay(4, 6), "米粉节", "米粉节"),
      (monthDay(5, 1), "劳动节", "五一国际劳动节|51国际劳动节|国际劳动节|五一劳动节|劳动节|五一"),
      (monthDay(5, 12), "护士节", "护士节"),
      (monthDay(5, 18), "五二零", "五二零"),
      (monthDay(5, 18), "国际博物馆日", "国际博物馆日|博物馆日"),
      (monthDay(5, 2), "世界哮喘日", "世界哮喘日"),
      (monthDay(5, 25), "国际失踪儿童日", "国际失踪儿童日"),
      (monthDay(5, 28), "全国爱发日", "全国爱发日"),
      (monthDay(5, 31), "世界无烟日", "世界无烟日"),
      (monthDay(5, 4), "五四青年节", "中国五四青年节|五四青年节|中国54青年节|54青年节|中国青年节|青年节"),
      (monthDay(5, 8), "世界红十字日", "世界红十字日"),
      (monthDay(6, 1), "儿童节", "国际六一儿童节|六一儿童节|国际61儿童节|61儿童节|儿童节|六一"),
      (monthDay(4, 4), "台湾儿童节", "台湾儿童节"),
      (monthDay(6, 11), "中国人口日", "中国人口日"),
      (monthDay(6, 14), "世界献血日", "世界献血日"),
      (monthDay(6, 15), "健康素食日", "健康素食日"),
      (monthDay(6, 17), "世界防治荒漠化和干旱日", "世界防治荒漠化和干旱日"),
      (monthDay(6, 20), "世界难民日", "世界难民日"),
      (monthDay(6, 21), "国际瑜伽日", "国际瑜伽日"),
      (monthDay(6, 23), "奥林匹克日", "奥林匹克日"),
      (monthDay(6, 25), "全国土地日", "全国土地日"),
      (monthDay(6, 26), "国际禁毒日", "国际禁毒日|禁毒日"),
      (monthDay(6, 5), "世界环境日", "世界环境日"),
      (monthDay(6, 7), "高考", "高考"),
      (monthDay(6, 8), "世界海洋日", "世界海洋日"),
      (monthDay(7, 1), "建党节", "七一|七一节|建党节|七一建党节"),
      (monthDay(7, 1), "香港回归纪念日", "香港回归纪念日"),
      (monthDay(7, 1), "中国共产党的生日", "中国共产党诞生日|党的生日"),
      (monthDay(7, 11), "世界人口日", "世界人口日"),
      (monthDay(7, 11), "中国航海节", "中国航海节"),
      (monthDay(7, 28), "世界肝炎日", "世界肝炎日"),
      (monthDay(7, 4), "美国独立日", "美国独立日|独立日"),
      (monthDay(7, 7), "抗战纪念日", "抗战纪念日"),
      (monthDay(8, 1), "建军节", "中国人民解放军建军节|八一建军节|建军节"),
      (monthDay(8, 12), "国际青年日", "国际青年日"),
      (monthDay(8, 12), "国际青年节", "国际青年节"),
      (monthDay(8, 15), "日本投降日", "日本投降日"),
      (monthDay(8, 8), "台湾父亲节", "台湾父亲节"),
      (monthDay(8, 8), "全民健身日", "全民健身日"),
      (monthDay(9, 1), "中小学开学日", "中小学开学日"),
      (monthDay(9, 10), "教师节", "中国教师节|教师节"),
      (monthDay(9, 16), "国际臭氧层保护日", "国际臭氧层保护日"),
      (monthDay(9, 17), "世界清洁地球日", "世界清洁地球日"),
      (monthDay(9, 18), "九一八事变纪念日", "九一八事变纪念日"),
      (monthDay(9, 20), "全国爱牙日", "全国爱牙日"),
      (monthDay(9, 21), "国际和平日", "国际和平日"),
      (monthDay(9, 22), "世界无车日", "世界无车日"),
      (monthDay(9, 26), "世界避孕日", "世界避孕日"),
      (monthDay(9, 27), "世界旅游日", "世界旅游日"),
      (monthDay(9, 28), "台湾教师节", "台湾教师节|教师节"),
      (monthDay(9, 29), "世界心脏日", "世界心脏日"),
      (monthDay(9, 3), "抗战胜利日", "抗战胜利日"),
      (monthDay(9, 5), "国际慈善日", "国际慈善日"),
      (monthDay(9, 8), "国际扫盲日", "国际扫盲日"),
      // Fixed day/week/month, year over year
      (nthDayOfWeekOfMonth(1, 3, MONDAY), "马丁路德金日", "马丁路德金纪念日|马丁路德金日"),
      (nthDayOfWeekOfMonth(5, 2, SUNDAY), "母亲节", "母亲节"),
      (nthDayOfWeekOfMonth(6, 3, SUNDAY), "父亲节", "父亲节"),
      (nthDayOfWeekOfMonth(11, 4, THURSDAY), "感恩节", "感恩节"),
      (nthDayOfWeekOfMonth(11, 4, FRIDAY), "黑色星期五", "黑色星期五|黑五"),
      (nthDayOfWeekOfMonth(1, 1, SUNDAY), "黑人日", "黑人日|黑人节"),
      (nthDayOfWeekOfMonth(4, 3, SUNDAY), "世界儿童日", "世界儿童日"),
      (nthDayOfWeekOfMonth(5, 2, SUNDAY), "救助贫困母亲日", "救助贫困母亲日"),
      (nthDayOfWeekOfMonth(5, 3, TUESDAY), "世界牛奶日", "国际牛奶日|世界牛奶日"),
      (nthDayOfWeekOfMonth(5, 3, SUNDAY), "全国助残日", "全国助残日"),
      (nthDayOfWeekOfMonth(7, 1, SATURDAY), "国际合作节", "国际合作节"),
      (nthDayOfWeekOfMonth(9, 3, SATURDAY), "全民国防教育日", "全民国防教育日"),
      (nthDayOfWeekOfMonth(9, 4, SUNDAY), "国际聋人节", "国际聋人节|国际聋人日")
    )
  )

  def mkRuleHolidays(list: List[(TimeData, String, String)]): List[(Token, String, String)] = {
    list.map {
      case (td, name, ptn) =>
        val token = tt(td.copy(okForThisNext = true, holiday = name, hint = Hint.Holiday))
        (token, name, ptn)
    }
  }

  def build(holidayList: List[(Token, String, String)]): (ImmutableMap[String, Token], java.util.TreeMap[String, String]) = {
    val tokenMap = mutable.Map[String, Token]()
    val normalMap = mutable.Map[String, mutable.Set[String]]()
    holidayList.foreach{
      case (tk, name, ptn) =>
        tokenMap.put(name, tk)

        for (currPattern <- ptn.split("\\|")) {
          val patternSet = normalMap.getOrElse(currPattern, mutable.Set[String]())
          patternSet.add(name)
          normalMap.put(currPattern, patternSet)
        }
    }

    val builder = Maps.newTreeMap[String, String]()
    normalMap.foreach{
      case (pt, normalSet) => normalSet.foreach(normal =>
        if (!builder.containsKey(pt) || builder.containsKey(pt) && pt == normal) {
          builder.put(pt, normal)
        }
      )
    }
    val tokenMapBuilder = ImmutableMap.builder[String, Token]()
    tokenMap.toMap.foreach{case (k, v) => tokenMapBuilder.put(k, v)}

    (tokenMapBuilder.build(), builder)
  }

  val (tokenMap, dictBuilder) = build(rulePeriodicHolidays ++ LunarDays.rulePeriodicHolidays)

  override def dict: Dict = new Dict(dictBuilder, false)

  override def holidayTokenMap: ImmutableMap[String, Token] = tokenMap
}

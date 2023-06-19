# 支持的Dimension

## Time/Duration/Date

Date作为子集专门抽取出来是是为了在只需要日期的时候提高效率。

Duration与时间点、时间区间并不等同，区分开来更明确。

返回结果TimeValue的结构包括了几种情况

```scala
case class TimeValue(timeValue: SingleTimeValue, // 时间，可能是时间点，也可能是时间区间
                     holiday: Option[String] // 节假日名称
                    )

trait SingleTimeValue
// 时间点
case class SimpleValue(instant: InstantValue) extends SingleTimeValue
// 时间区间（左闭右开）
case class IntervalValue(start: InstantValue, end: InstantValue) extends SingleTimeValue
// 无上/下限时间区间
case class OpenIntervalValue(start: InstantValue, direction: IntervalDirection)
      extends SingleTimeValue
```

时间作为最复杂最基础的Dimension，用duckling来实现有以下几点值得一提：

1. 一致的定义，所有时间区间都是左闭右开； 

2. 在没有指定范围时，总是取下一个即将发生的； 

3. 区分时间点/时间区间与持续时间 

4. 由CFG保证的规则复用 

5. 相对高得多的测试覆盖

6. 由PCFG保证的组合结果择优 

7. 纯函数式实现，没有线程安全类的问题； 

8. 可以接受的性能 

| Sub-Dimension | 示例           | 解析结果示意                                                 |
| ------------- | -------------- | ------------------------------------------------------------ |
| Date          | 2015年         | {timeValue: {instant: {datetime: 2015-01-01, grain: Year}}}  |
|               | 2015-03-03     | {timeValue: {instant: {datetime: 2015-03-03, grain: Day}}}   |
| Duration      | 1秒钟          | {value: 1, grain: Second}                                    |
|               | 两年零三个月   | {value: 27, grain: Month}                                    |
| Time          | 今天           | {timeValue: {instant: {datetime: 2013-02-12, grain: Day}}}   |
|               | 前两个月       | {<br/>        "timeValue": {<br/>          "start": {<br/>            "datetime": "2019-09-01T00:00+08:00[Asia/Shanghai]",<br/>            "grain": "Month"<br/>          },<br/>          "end": {<br/>            "datetime": "2019-11-01T00:00+08:00[Asia/Shanghai]",<br/>            "grain": "Month"<br/>          }<br/>        }<br/>      } |
|               | 下午三点十五   | {<br/>  "instant": {<br/>    "datetime": "2019-11-28T15:15+08:00[Asia/Shanghai]",<br/>    "grain": "Minute"<br/>  }<br/>} |
| 节日          | 儿童节         | {<br/>  "timeValue": {<br/>    "instant": {<br/>      "datetime": "2020-06-01T00:00+08:00[Asia/Shanghai]",<br/>      "grain": "Day"<br/>    }<br/>  },<br/>  "holiday": "儿童节"<br/>} |
|               | 父亲节         | {<br/>  "timeValue": {<br/>    "instant": {<br/>      "datetime": "2020-06-21T00:00+08:00[Asia/Shanghai]",<br/>      "grain": "Day"<br/>    }<br/>  },<br/>  "holiday": "父亲节"<br/>} |
| 农历          | 农历八月初八   | {<br/>  "timeValue": {<br/>    "instant": {<br/>      "datetime": "2020-09-24T00:00+08:00[Asia/Shanghai]",<br/>      "grain": "Day"<br/>    }<br/>  }<br/>} |
| 农历节日      | 春节           | {<br/>  "timeValue": {<br/>    "instant": {<br/>      "datetime": "2020-01-25T00:00+08:00[Asia/Shanghai]",<br/>      "grain": "Day"<br/>    }<br/>  },<br/>  "holiday": "春节"<br/>} |
| 节气          | 2019年的清明节 | {<br/>  "timeValue": {<br/>    "instant": {<br/>      "datetime": "2019-04-05T00:00+08:00[Asia/Shanghai]",<br/>      "grain": "Day"<br/>    }<br/>  },<br/>  "holiday": "清明"<br/>} |

注意：时间解析在Options中额外定义了改变行为的参数TimeOptions

| 参数           | 意义                                             | 默认值 |
| -------------- | ------------------------------------------------ | ------ |
| resetTimeOfDay | 上午是否总是需要指今天的上午，默认是未来一个上午 | False  |
| recentInFuture | 最近是向前计算还是向后计算，默认是未来           | True   |

## Numeral

返回结果定义，分为了三种情况。

```scala
trait IntervalValue extends ResolvedValue

// 具体值
case class NumeralValue(n: Double) extends IntervalValue
// 区间
case class DoubleSideIntervalValue(left: Double,
                                   right: Double,
                                   leftType: IntervalType = IntervalType.Closed,
                                   rightType: IntervalType = IntervalType.Open)
  extends IntervalValue
// 无上下限区间
case class OpenIntervalValue(start: Double, direction: IntervalDirection) extends IntervalValue
```



| 分类     | 示例             | 解析结果                   |
| -------- | ---------------- | -------------------------- |
| Fraction | 百分之六十       | {<br/>  "n": 0.6<br/>}     |
|          | 一半             | {<br/>  "n": 0.5<br/>}     |
| Numeral  | 零零点零零二八五 | {<br/>  "n": 0.00285<br/>} |
|          | 一万两千零3十四  | {<br/>  "n": 12034<br/>}   |


## Act

| 示例           | 解析结果示例                                                 |
| -------------- | ------------------------------------------------------------ |
| 倒数第一幕     | {<br/>  "v": -1,<br/>  "unit": "场",<br/>  "dim": "场"<br/>} |
| 第一百一十一场 | {<br/>  "v": 111,<br/>  "unit": "场",<br/>  "dim": "场"<br/>} |

## Age


| 示例       | 解析结果示例（示意）                                     |
| ---------- | -------------------------------------------------------- |
| 38岁半     | {n:38.5}                                                 |
| 大于35岁   | {start:35, direction: After}                             |
| 不高于35岁 | {start:35, direction: Before}                            |
| 三到五岁半 | {left:3, right:5.5, leftType: Closed, rightType: Closed} |

## Currency

| 示例             | 解析结果示例（示意）                                         |
| ---------------- | ------------------------------------------------------------ |
| 九十九元九角九分 | {<br/>  "v": 99.99,<br/>  "unit": "元",<br/>  "dim": "货币:*"<br/>} |
| 九毛钱           | {<br/>  "v": 0.9,<br/>  "unit": "元",<br/>  "dim": "货币:*"<br/>} |

## Episode

| 示例           | 解析结果示例                                                 |
| -------------- | ------------------------------------------------------------ |
| 倒数第一集     | {<br/>  "v": -1,<br/>  "unit": "集",<br/>  "dim": "集"<br/>} |
| 第一百一十一期 | {<br/>  "v": 111,<br/>  "unit": "集",<br/>  "dim": "集"<br/>} |

## Level

|         |                       |
| ------- | --------------------- |
| 第五档  | {<br/>  "n": 5<br/>}  |
| 三十3级 | {<br/>  "n": 33<br/>} |

## Velocity

| 示例        | 解析结果示例（示意）                                         |
| ----------- | ------------------------------------------------------------ |
| 3千米每小时 | {<br/>  "v": 3,<br/>  "unit": "千米每小时",<br/>  "dim": "Velocity"<br/>} |
| 每秒1米     | {<br/>  "v": 1,<br/>  "unit": "米每秒",<br/>  "dim": "Velocity"<br/>} |

## Rating

| 示例            | 解析结果示例（示意）          |
| --------------- | ----------------------------- |
| 评分8点5分      | {n:38.5}                      |
| 评分在8.5分以上 | {start:8.5, direction: After} |

## Season

| 示例            | 解析结果示例（示意）          |
| --------------- | ----------------------------- |
| 倒数第一季      | {<br/>  "v": -1,<br/>  "unit": "季",<br/>  "dim": "季"<br/>} |
| 第三季 | {<br/>  "v": -1,<br/>  "unit": "季",<br/>  "dim": "季"<br/>} |

## Temperature

| 示例            | 解析结果示例（示意）          |
| --------------- | ----------------------------- |
| 摄氏30度 | {<br/>  "v": -1,<br/>  "unit": "C",<br/>  "dim": "温度"<br/>} |
| 华氏22点6度 | {<br/>  "v": 22.6,<br/>  "unit": "F",<br/>  "dim": "温度"<br/>} |


## 2. 更多Dimension

2.0 序数

2.1 星座

2.2 地点

2.3 性别

2.4 音乐歌词的参与者



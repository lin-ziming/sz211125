package com.atguigu.gmall.realtime.demos

import java.text.SimpleDateFormat
import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.Date

/**
 * Created by Smexy on 2022/4/24
 *
 *    日期对象: Date
 *
 *    日期格式: SimpleDateFormat
 *
 *    实例方法!
 *
 *    -----------------推荐java 8之后提供的java.time包下的新的api-------------
 *
 *     日期对象: LocalDate 或  LocalDateTime（带时分秒）
 *
 *      日期格式: DateTimeFormater
 *
 *      都是静态方法
 */
object TimeFieldParse {

  def main(args: Array[String]): Unit = {

    val ts=1650617070000l

    val date = new Date(ts)

    val format1 = new SimpleDateFormat("yyyy-MM-dd")
    val format2 = new SimpleDateFormat("HH")

    val dateStr: String = format1.format(date)
    val hourStr: String = format2.format(date)

    println(dateStr)
    println(hourStr)

    println("----------------------")

    val formatter1: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val formatter2: DateTimeFormatter = DateTimeFormatter.ofPattern("HH")

    val dateTime: LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.of("Asia/Shanghai"))

    val dateStr2: String = dateTime.format(formatter1)
    val hourStr2: String = dateTime.format(formatter2)

    println(dateStr2)
    println(hourStr2)


  }

}

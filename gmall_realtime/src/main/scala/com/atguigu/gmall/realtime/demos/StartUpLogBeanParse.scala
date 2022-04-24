package com.atguigu.gmall.realtime.demos

import java.lang
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId}

import com.alibaba.fastjson.{JSON, JSONObject}
import com.atguigu.gmall.realtime.beans.{StartLogInfo, StartUpLog}

/**
 * Created by Smexy on 2022/4/24
 */
object StartUpLogBeanParse{

  def main(args: Array[String]): Unit = {


    val str=
      """
        |
        |{"common":{"ar":"110000","ba":"Xiaomi","ch":"xiaomi","is_new":"1","md":"Xiaomi 10 Pro ","mid":"mid_472","os":"Android 10.0","uid":"561","vc":"v2.1.134"},
        |"start":{"entry":"notice","loading_time":5091,"open_ad_id":19,"open_ad_ms":3763,"open_ad_skip_ms":0},"ts":1650617070000}
        |
        |""".stripMargin

    //单独从str中取出common部分进行封装

    val jsonMap: JSONObject = JSON.parseObject(str)

    val startUpLog: StartUpLog = JSON.parseObject(jsonMap.getString("common"), classOf[StartUpLog])

    // 将start部分封装为StartLogInfo类型，合并到startUpLog中
    val startLogInfo: StartLogInfo = JSON.parseObject(jsonMap.getString("start"), classOf[StartLogInfo])

    startUpLog.mergeStartInfo(startLogInfo)

    // 解析ts
    val formatter1: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val formatter2: DateTimeFormatter = DateTimeFormatter.ofPattern("HH")

    val ts: lang.Long = jsonMap.getLong("ts")

    startUpLog.ts = ts

    val dateTime: LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.of("Asia/Shanghai"))

    startUpLog.logDate = dateTime.format(formatter1)
    startUpLog.logHour = dateTime.format(formatter2)


    println(startUpLog)


  }


}

package com.atguigu.gmall.realtime.apps

import java.lang
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId}

import com.alibaba.fastjson.{JSON, JSONObject}
import com.atguigu.gmall.constants.{PrefixConstant, TopicConstant}
import com.atguigu.gmall.realtime.beans.{StartLogInfo, StartUpLog}
import com.atguigu.gmall.realtime.utils.{MyKafkaUtil, RedisUtil}
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.{CanCommitOffsets, HasOffsetRanges, OffsetRange}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import redis.clients.jedis.Jedis



/**
 * Created by Smexy on 2022/4/24
 */
object DAUApp extends BaseApp {

  override var groupName: String = "sz1125"
  override var batchDuration: Int = 10
  override var appName: String = "DAUApp"

  def parseBean(rdd: RDD[ConsumerRecord[String, String]]):RDD[StartUpLog] = {

    rdd.map(record => {

      val jsonMap: JSONObject = JSON.parseObject(record.value())

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

      startUpLog

    })

  }

  def removeDuplicateRecordInCurrentBatch(rdd: RDD[StartUpLog]):RDD[StartUpLog] = {

    // 把同一天，同一个设备在当前批次的启动日志放入一个组中
    val rdd1: RDD[((String, String), Iterable[StartUpLog])] = rdd.map(log => ((log.logDate, log.mid), log)).groupByKey()

    //每个组选ts最小那条
    val rdd2: RDD[StartUpLog] = rdd1.flatMap {
      case ((date, mid), logs) => logs.toList.sortBy(_.ts).take(1)
    }
    rdd2
  }

  /*
      key:  DAU:日期
      value:  Set(mid_id)


      只要是连接数据库的操作，都使用 xxxPartition()算子!
          一个分区使用一个连接，节省创建连接带来的开销

          mapPartitions:  有返回值
          foreachPartition: 没返回值

   */
  def removeHistoryBatchRecord(rdd: RDD[StartUpLog]): RDD[StartUpLog] = {

    rdd.mapPartitions(partition => {

      //创建连接
      val jedis: Jedis = RedisUtil.getJedisClient()

      //使用 判断当前rdd中的这条log的Mid是否在redis中已经存在，如果已经存在说明这个设备今天已经记录过了
      val iterator: Iterator[StartUpLog] = partition.filter(log => !jedis.sismember(PrefixConstant.dau_redis_Preffix + log.logDate, log.mid))

      // 关闭连接
      jedis.close()

      iterator

    })

  }

  def main(args: Array[String]): Unit = {

      // 覆盖父类中的StreamingContext
     context = new StreamingContext("local[*]",appName,Seconds(batchDuration))

     runApp{

       //传入自己的计算逻辑
       // 获取DS
       val ds: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream(Array(TopicConstant.STARTUP_LOG), context, groupName)

       ds.foreachRDD(rdd => {
         
         if (!rdd.isEmpty()){

           //获取偏移量
           val ranges: Array[OffsetRange] = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
           
           // 转换样例类
           val rdd1: RDD[StartUpLog] = parseBean(rdd)

           // 当前批次去重
           val rdd2: RDD[StartUpLog] = removeDuplicateRecordInCurrentBatch(rdd1)

           // 和历史批次进行比对，如果当前设备在历史批次已经记录过了今天的最早日期，当前批次就无需再次记录
           val rdd3: RDD[StartUpLog] = removeHistoryBatchRecord(rdd2)

           //导入phonix提供的所有的针对spark集成的静态方法
           import org.apache.phoenix.spark._

           rdd3.cache()

           println("即将写入hbase:"+rdd3.count())

           /*
                参数1： 要写入的表名
                参数2:   cols: Seq[String]
            */
           rdd3.saveToPhoenix("gmall2022_startuplog",
             Seq("AR", "BA", "CH", "IS_NEW", "MD", "MID", "OS", "UID", "VC", "ENTRY", "LOADING_TIME","OPEN_AD_ID","OPEN_AD_MS","OPEN_AD_SKIP_MS","LOGDATE","LOGHOUR","TS"),
             //不仅读取hadoop的配置文件，也会读取hbase的配置文件
             HBaseConfiguration.create(),
             Some("hadoop103:2181")
           )

           // 把已经在hbase中记录过日志的mid写入redis
           rdd3.foreachPartition(partition => {

             val jedis: Jedis = RedisUtil.getJedisClient()

             partition.foreach(log => jedis.sadd(PrefixConstant.dau_redis_Preffix + log.logDate, log.mid))

             jedis.close()

           })

           //提交偏移量
           ds.asInstanceOf[CanCommitOffsets].commitAsync(ranges)

         }
       })
     }
  }

}

package com.atguigu.gmall.realtime.apps

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}
import java.util

import com.alibaba.fastjson.{JSON, JSONObject}
import com.atguigu.gmall.constants.TopicConstant
import com.atguigu.gmall.realtime.apps.DAUApp.{context, groupName}
import com.atguigu.gmall.realtime.beans.{Action, ActionsLog, CommonInfo, CouponAlertInfo}
import com.atguigu.gmall.realtime.utils.MyKafkaUtil
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{CanCommitOffsets, HasOffsetRanges, OffsetRange}
import org.apache.spark.streaming.{Minutes, Seconds, StreamingContext}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks

/**
 * Created by Smexy on 2022/4/28
 *
 *    SparkStreaming中的三个时间单位:
 *        批次采集时间  batchDuration:  在构建StreamingContext时指定。
 *                                  默认  window = slide = batchDuration
 *        计算的数据时间范围 window:    通过window算子指定
 *        Job提交的时间间隔  slide:     通过window算子指定
 *            window 和 slide 必须是 batchDuration的整倍数
 *
 *  -----------------------
 *    at least once + 幂等输出(es支持)
 */
object AlertApp extends BaseApp {
  override var groupName: String = "sz1125"
  override var batchDuration: Int = 10
  override var appName: String = "AlertApp"

  def parseRecord(rdd: RDD[ConsumerRecord[String, String]]): RDD[ActionsLog] = {

    rdd.map(record => {

      val map: JSONObject = JSON.parseObject(record.value())
      //获取common部分
      val commonInfo: CommonInfo = JSON.parseObject(map.getString("common"), classOf[CommonInfo])

      import collection.JavaConverters._
      // 获取actions部分  把java中的list 转为scala中的list
      val actions: List[Action] = JSON.parseArray(map.getString("actions"), classOf[Action]).asScala.toList

      ActionsLog(actions,map.getLong("ts"),commonInfo)

    })




  }

  def main(args: Array[String]): Unit = {

    //需要在Spark中设置ES集群的参数
    // https://www.elastic.co/guide/en/elasticsearch/hadoop/6.6/spark.html
    val sparkConf: SparkConf = new SparkConf().setMaster("local[*]").setAppName(appName)

    sparkConf.set("es.nodes","hadoop102,hadoop103,hadoop104")
    sparkConf.set("es.port ","9200")
    sparkConf.set("es.index.auto.create", "true")

    context = new StreamingContext(sparkConf, Seconds(batchDuration))

    runApp{

      // 获取DS
      val ds: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream(Array(TopicConstant.ACTIONS_LOG), context, groupName)

      var ranges: Array[OffsetRange] = null

      // 获取偏移量和转换样例类
      val ds1: DStream[ActionsLog] = ds.transform(rdd => {

        ranges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

        //将ConsumerRecord 转换为样例类
        val rdd1: RDD[ActionsLog] = parseRecord(rdd)
        rdd1

      })

      // 采集过去5分钟的数据
      val ds2: DStream[((String, Long), Iterable[ActionsLog])] = ds1.window(Minutes(5))
        .map(actionsLog => ((actionsLog.common.mid, actionsLog.common.uid), actionsLog))
        //按照 设备和用户分组
        .groupByKey()

      // 有嫌疑的数据
      val ds3: DStream[((String, Long), Iterable[ActionsLog])] = ds2.filter {
        case ((mid, uid), actionsLogs) => {

          //假设这个用户是没有嫌疑的
          var ifNeedAlert = false

          Breaks.breakable {

            actionsLogs.foreach(actionsLog => actionsLog.actions.foreach(
              action => {
                //判断是否增加了收货地址
                if ("trade_add_address".equals(action.action_id)) {
                  ifNeedAlert = true
                  //后续是无需再判断他的行为
                  Breaks.break()
                }

              }

            ))
          }

          ifNeedAlert

        }
      }

      // 将有嫌疑的用户及其日志按照设备进行分组
      val ds4: DStream[(String, Iterable[Iterable[ActionsLog]])] = ds3.map {
        case ((mid, uid), actionsLogs) => (mid, actionsLogs)
      }.groupByKey()

      // 需要预警的数据
      val ds5: DStream[(String, Iterable[ActionsLog])] = ds4.filter(_._2.size >= 2)
        .mapValues(_.flatten)

      // 产生预警日志
      val ds6: DStream[CouponAlertInfo] = ds5.map {
        case (mid, actionsLogs) => {

          var uids: mutable.Set[String] = mutable.Set[String]()
          var itemIds: mutable.Set[String] = mutable.Set[String]()
          var events: ListBuffer[String] = ListBuffer[String]()

          actionsLogs.foreach(actionsLog => {

            uids.add(actionsLog.common.uid.toString)
            actionsLog.actions.foreach(

              action => {

                events.append(action.action_id)

                //如果用户在此期间收藏过商品，记录收藏的商品id
                if ("favor_add".equals(action.action_id)) {

                  itemIds.add(action.item)

                }

              }

            )


          })

          /*
            并且同一设备，每分钟只记录一次预警。

            id怎么写? 能保证每分钟只记录一次预警
                id 必须包含 mid ，还需要保证一个mid如果一分钟产生了多条预警日志，最终只记录一条到数据库

            PUT /xxxindex/mid_分钟

            PUT /xxxindex/mid101_2022-04-28_16:32, 15log1
            PUT /xxxindex/mid101_2022-04-28_16:32, 40log2

            生成预警日志
           */
          val ts: Long = System.currentTimeMillis()
          val dateTime: LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.of("Asia/Shanghai"))
          val formatter1: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
          val minuteStr: String = dateTime.format(formatter1)

          CouponAlertInfo(mid + "_" + minuteStr, uids, itemIds, events, ts)


        }
      }

      // 向es写入
      import org.elasticsearch.spark._

      ds6.foreachRDD(rdd => {

        rdd.cache()

        println("即将写入:"+rdd.count())

        // es.mapping.id 要把rdd中的 Bean的哪个属性作为 es中的id存入
        rdd.saveToEs("/gmall_behaviour_alert"+LocalDate.now()+"/_doc", Map("es.mapping.id" -> "aid"))

        //提交偏移量
        ds.asInstanceOf[CanCommitOffsets].commitAsync(ranges)

      })




    }

  }
}

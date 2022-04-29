package com.atguigu.gmall.realtime.apps

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import java.util

import com.alibaba.fastjson.JSON
import com.atguigu.gmall.constants.{PrefixConstant, TopicConstant}
import com.atguigu.gmall.realtime.apps.AlertApp.{appName, batchDuration, context, groupName}
import com.atguigu.gmall.realtime.beans.{OrderDetail, OrderInfo, SaleDetail, UserInfo}
import com.atguigu.gmall.realtime.utils.{MyKafkaUtil, RedisUtil}
import com.google.gson.Gson
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{CanCommitOffsets, HasOffsetRanges, OffsetRange}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import redis.clients.jedis.Jedis

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks

/**
 * Created by Smexy on 2022/4/29
 *
 *  at least once + 幂等输出(ES) 实现精确一次消费
 *
 *      双流Join的前提:
 *          ①两个流必须从同一个StreamingContext 获取
 *          ②只有 DS[K,V]才能Join
 *                join的原理是把相同key的不同DS的V放在一起
 *                DS1[K,V1] join  DS2[K,V2] = DS[K,(V1,V2)]
 *
 *    -------------------
 *      无法Join的根本原因在于 要Join的数据在不同的批次被消费到，错过了!
 *
 *      ------------
 *        解决思路： 早到的数据，在缓存中等候后续批次数据进行Join
 *
 *        --------
 *        实现步骤:
 *            OrderInfo:  1笔orderInfo对应 N个orderDetail
 *                      ①和当前批次已经到的orderDetail进行关联
 *                      ②不确定后续是否还有晚到的orderDetail，无条件写入缓存，等待后续出现的orderDetail
 *                      ③可能会有早到的OrderDetail，去缓存中读取早到的orderDetail，进行join
 *
 *
 *            orderDetail:
 *                        ①和当前批次已经到的orderInfo进行关联(和orderInfo的①是一样的，无需重复实现)
 * *                      ②如果①关联不上，说明orderDetail可能早到或晚到了，
 *                            去缓存读取对应的OrderInfo，读到了，说明当前的orderDetail晚到，
 *                         ③否则，读不到，说明OrderDetail早到了，需要写入缓存等后续批次的OrderInfo
 *
 *
 *            -----------
 *
 * 一天的订单，存储一个k-v
 *      orderInfo: 1  orderInfo: 2
 *
 *
 *  key: name: day_日期
 *
 *    {
       * "order_id_1":"JSON字符串",
       * "order_id_2":"JSON字符串"
 *
 *      }
 *
 *  hash : 是干嘛的?
 *              如果要经常修改redis中某个对象的属性! 存储为hash
 *
 *              只读: string
 *
 *
 *         ------------
 *
 * key:  orderdetail:order_id
 * value:  set
 * *
 *
 */
object SaleDetailApp extends  BaseApp {
  override var groupName: String = "sz1125group1"
  override var batchDuration: Int = 10
  override var appName: String = "SaleDetailApp"

  def parseOrderInfoRecord(rdd: RDD[ConsumerRecord[String, String]]): RDD[(String,OrderInfo)] = {

    rdd.map(record => {

      val orderInfo: OrderInfo = JSON.parseObject(record.value(), classOf[OrderInfo])

      // 时间: create_time": "2022-04-27 03:18:39",
      val f1: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      val f2: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
      val f3: DateTimeFormatter = DateTimeFormatter.ofPattern("HH")

      val localDateTime: LocalDateTime = LocalDateTime.parse(orderInfo.create_time, f1)
      orderInfo.create_date=localDateTime.format(f2)
      orderInfo.create_hour=localDateTime.format(f3)

      (orderInfo.id ,orderInfo)

    })


  }


  def main(args: Array[String]): Unit = {

    val sparkConf: SparkConf = new SparkConf().setMaster("local[*]").setAppName(appName)

    sparkConf.set("es.nodes","hadoop102,hadoop103,hadoop104")
    sparkConf.set("es.port ","9200")
    sparkConf.set("es.index.auto.create", "true")

    context = new StreamingContext(sparkConf, Seconds(batchDuration))

    runApp{

      //消费两个主题，获取两个流
      val orderInfoDs: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream(Array(TopicConstant.GMALL_ORDER_INFO), context, groupName)
      val orderDetailDs: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream(Array(TopicConstant.GMALL_ORDER_DETAIL), context, groupName)

      var orderInfoRanges: Array[OffsetRange] = null
      var orderDetailRanges: Array[OffsetRange] = null
      //获取偏移量
      val ds1: DStream[(String, OrderInfo)] = orderInfoDs.transform(rdd => {

        orderInfoRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

        parseOrderInfoRecord(rdd)

      })

      val ds2: DStream[(String, OrderDetail)] = orderDetailDs.transform(rdd => {

        orderDetailRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

        rdd.map(record => {
          val orderDetail: OrderDetail = JSON.parseObject(record.value(), classOf[OrderDetail])
          (orderDetail.order_id, orderDetail)
        })

      })

      // 最终要的是购物详情  order_detail ，为order_detail 获取 Order_info 的uid
      val ds3: DStream[(String, (Option[OrderInfo], Option[OrderDetail]))] = ds1.fullOuterJoin(ds2)

      // ds4保存的是关联上的SaleDetail，有uid
      val ds4: DStream[SaleDetail] = ds3.mapPartitions(partition => {

        //准备一个集合，存放封装好的购物详情
        val saleDetails: ListBuffer[SaleDetail] = ListBuffer[SaleDetail]()

        val jedis: Jedis = RedisUtil.getJedisClient()

        val gson = new Gson()

        partition.foreach {
          case (orderId, (orderInfoOption, orderDetailOption)) => {

            if (orderInfoOption != None) {

              val orderInfo: OrderInfo = orderInfoOption.get

              //判断右侧的OrderDetail是否为None
              if (orderDetailOption != None) {

                val orderDetail: OrderDetail = orderDetailOption.get

                // ①和当前批次已经到的orderDetail进行关联
                saleDetails.append(new SaleDetail(orderInfo, orderDetail))

              }

              //②不确定后续是否还有晚到的orderDetail，无条件写入缓存，等待后续出现的orderDetail
              jedis.set(PrefixConstant.order_info_redis_preffix + orderInfo.id, gson.toJson(orderInfo))

              //③可能会有早到的OrderDetail，去缓存中读取早到的orderDetail，进行join
              val earlyComingOrderDetails: util.Set[String] = jedis.smembers(PrefixConstant.order_detail_redis_preffix + orderId)

              earlyComingOrderDetails.forEach(orderDetailStr => {

                val orderDetail: OrderDetail = JSON.parseObject(orderDetailStr, classOf[OrderDetail])

                saleDetails.append(new SaleDetail(orderInfo, orderDetail))

              })


            } else {

              val orderDetail: OrderDetail = orderDetailOption.get

              // 去缓存读取对应的OrderInfo，读到了，说明当前的orderDetail晚到，
              val orderInfoStr: String = jedis.get(PrefixConstant.order_info_redis_preffix + orderDetail.order_id)

              if (orderInfoStr != null) {

                val orderInfo: OrderInfo = JSON.parseObject(orderInfoStr, classOf[OrderInfo])

                saleDetails.append(new SaleDetail(orderInfo, orderDetail))

              } else {

                //在缓存中找不到早到的orderInfo,否则，读不到，说明OrderDetail早到了，需要写入缓存等后续批次的OrderInfo
                jedis.sadd(PrefixConstant.order_detail_redis_preffix + orderDetail.order_id, gson.toJson(orderDetail))

              }

            }


          }

        }

        jedis.close()

        saleDetails.iterator


      })

      // 查询redis中的用户信息
      val ds5: DStream[SaleDetail] = ds4.mapPartitions(partition => {

        val jedis: Jedis = RedisUtil.getJedisClient()

        val saleDetails: Iterator[SaleDetail] = partition.map(saleDetail => {

          var userStr: String = null

          Breaks.breakable{

            while (true){

              Thread.sleep(1500)

              userStr = jedis.get(PrefixConstant.user_info_redis_preffix + saleDetail.user_id)

              if (userStr != null){

                Breaks.break()

              }
            }

          }
          // 保证用户信息已经查到
          val userInfo: UserInfo = JSON.parseObject(userStr, classOf[UserInfo])
          saleDetail.mergeUserInfo(userInfo)
          saleDetail

        })

        jedis.close()

        saleDetails.toIterator

      })

      import  org.elasticsearch.spark._

      // 写出到es
      ds5.foreachRDD(rdd => {

        rdd.cache()

        println("写出购物详情:"+rdd.count())

        //写出到es
        rdd.saveToEs("/gmall2022_sale_detail"+LocalDate.now()+"/_doc",Map("es.mapping.id" -> "order_detail_id"))

        //提交偏移量
        orderInfoDs.asInstanceOf[CanCommitOffsets].commitAsync(orderInfoRanges)
        orderDetailDs.asInstanceOf[CanCommitOffsets].commitAsync(orderDetailRanges)

      })

    }
  }
}

package com.atguigu.gmall.realtime.apps

import com.alibaba.fastjson.{JSON, JSONObject}
import com.atguigu.gmall.constants.{PrefixConstant, TopicConstant}
import com.atguigu.gmall.realtime.apps.DAUApp.{appName, batchDuration, context, groupName}
import com.atguigu.gmall.realtime.utils.{MyKafkaUtil, RedisUtil}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.{CanCommitOffsets, HasOffsetRanges, OffsetRange}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import redis.clients.jedis.Jedis

/**
 * Created by Smexy on 2022/4/29
 *
 *    at least once + 幂等输出(redis) 实现精确一次
 *
 *    ----------
 *    设置user在redis中的存储格式
 *        粒度:   一个用户是一个K-v
 *                    V:string
 *               所有用户是一个K-V
 *                    V: set
 *
 *    后续使用是根据userid查询redis，因此采取以下设计
 * key:  userinfo:userId
 * value:  string
 */
object UserApp extends BaseApp {
  override var groupName: String = "sz1125"
  override var batchDuration: Int = 5
  override var appName: String = "UserApp"

  def main(args: Array[String]): Unit = {

    context = new StreamingContext("local[*]",appName,Seconds(batchDuration))

    runApp{

      //传入自己的计算逻辑
      // 获取DS
      val ds: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream(Array(TopicConstant.GMALL_USER_INFO), context, groupName)

      ds.foreachRDD(rdd => {

        if (!rdd.isEmpty()) {

          //获取偏移量
          val ranges: Array[OffsetRange] = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

          rdd.foreachPartition(partition => {

            val jedis: Jedis = RedisUtil.getJedisClient()

            println("写出:"+partition.size)

            partition.foreach(record => {

              val map: JSONObject = JSON.parseObject(record.value())

              jedis.set(PrefixConstant.user_info_redis_preffix+map.getString("id"),record.value())


            })

            //不能在这里提交

            jedis.close()

          })

          //提交偏移量
          ds.asInstanceOf[CanCommitOffsets].commitAsync(ranges)

        }

      })


    }



  }
}

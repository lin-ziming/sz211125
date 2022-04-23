package com.atguigu.sparkstreaming.demos

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe

/**
 * Created by Smexy on 2022/4/23
 *
 *    精确一次性消费
 */
object SimpleDemo {

  def main(args: Array[String]): Unit = {

    val streamingContext = new StreamingContext("local[*]", "simpledemo", Seconds(5))


    // 准备消费者参数
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "hadoop102:9092,hadoop103:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "sz211125",
      // latest从最近位置消费，earliest:如果当前组从来没有消费过这个主题，从主题的最开始位置消费
      "auto.offset.reset" -> "latest",
      // 是否允许自动提交offset
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    // 指定要消费的主题  通常情况下，一个stream只消费一个topic
    val topics = Array("topicA")
    // 创建一个Stream
    val stream = KafkaUtils.createDirectStream[String, String](
      streamingContext,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )

    //对流进行计算
    val ds: DStream[String] = stream.map(record => record.value)

    ds.print(100)

    streamingContext.start()

    streamingContext.awaitTermination()

  }

}

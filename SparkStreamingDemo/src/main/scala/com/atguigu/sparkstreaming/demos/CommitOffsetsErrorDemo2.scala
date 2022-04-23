package com.atguigu.sparkstreaming.demos

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
 *
 *    提交偏移量注意事项:
 *        只有初始DS才能提交偏移量！
 *            只有初始DS才是DirectKafkaInputDStream类型，才能转换为CanCommitOffsets，才能调用commitAsync()
 *
 * Exception in thread "main" java.lang.ClassCastException:
 *    org.apache.spark.streaming.dstream.TransformedDStream
 *      cannot be cast to
 *    org.apache.spark.streaming.kafka010.CanCommitOffsets
 *
 *
 */
object CommitOffsetsErrorDemo2 {

  def main(args: Array[String]): Unit = {

    val streamingContext = new StreamingContext("local[*]", "simpledemo", Seconds(5))


    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "hadoop102:9092,hadoop103:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "sz211125",
      // latest从最近位置消费，earliest:如果当前组从来没有消费过这个主题，从主题的最开始位置消费
      "auto.offset.reset" -> "latest",
      // ******是否允许自动提交offset
      "enable.auto.commit" -> "false"
    )

    val topics = Array("topicA")

    val stream = KafkaUtils.createDirectStream[String, String](
      streamingContext,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )

    var ranges: Array[OffsetRange] = null

    val ds1: DStream[String] = stream.transform(rdd => {

      // 获取偏移量
     ranges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

      rdd.map(record => record.value())

    })

    ds1.foreachRDD(rdd => {

      rdd.foreach(str =>{

        println(Thread.currentThread().getName + "---->"+str)

      })

      //提交偏移量
      // ds1必须是DirectKafkaInputDStream类型，才能转换成功
      ds1.asInstanceOf[CanCommitOffsets].commitAsync(ranges)

    })


    streamingContext.start()

    streamingContext.awaitTermination()

  }

}

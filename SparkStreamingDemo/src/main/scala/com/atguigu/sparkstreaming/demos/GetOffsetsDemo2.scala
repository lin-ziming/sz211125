package com.atguigu.sparkstreaming.demos

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
 * Created by Smexy on 2022/4/23
 *
 *    偏移量是在Driver端获取!
 *
 *    只有写在算子中的代码才是在Executor端分布式运行!
 *
 *
 *
 *
 *
 *
 */
object GetOffsetsDemo2 {

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


  stream.foreachRDD { rdd =>

            if (!rdd.isEmpty()){

                  // 获取偏移量
                  // 判断当前的rdd是否是HasOffsetRanges的子类，如果是就强转为HasOffsetRanges类型之后调用offsetRanges方法，返回偏移量
                  // OffsetRange: 代表消费的主题的一个分区的元数据信息
                  val ranges: Array[OffsetRange] = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

                  for (elem <- ranges) {
                    // streaming-job-executor-0  Driver
                    println(Thread.currentThread().getName + "---->"+elem)
                  }

                  val rdd1: RDD[String] = rdd.map(record => record.value())

                  // Executor task launch worker for task 33  Executor端
                  rdd1.foreach(str => println(Thread.currentThread().getName + "---->"+str))
            }


    }


    streamingContext.start()

    streamingContext.awaitTermination()

  }

}

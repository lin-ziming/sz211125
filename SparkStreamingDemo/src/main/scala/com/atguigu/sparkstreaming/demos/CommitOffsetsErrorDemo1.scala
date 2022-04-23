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
 *
 *    提交偏移量注意事项:
 *            只能在Driver端提交偏移量，不能在算子中提交!
 *
 *
 */
object CommitOffsetsErrorDemo1 {

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
        val ranges: Array[OffsetRange] = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

        // 转换运算
        val rdd1: RDD[String] = rdd.map(record => record.value())

        // 输出到控制台
        rdd1.foreach(str =>{

          println(Thread.currentThread().getName + "---->"+str)


          // Caused by: java.io.NotSerializableException:
          //        Object of org.apache.spark.streaming.kafka010.DirectKafkaInputDStream is being serialized
          // possibly as a part of closure of an RDD operation. This is because
          // the DStream object is being referred to from within the closure.
          // Please rewrite the RDD operation inside this DStream to avoid this.
          // This has been enforced to avoid bloating of Spark tasks  with unnecessary objects.
          //提交偏移量
          stream.asInstanceOf[CanCommitOffsets].commitAsync(ranges)

        })



      }


    }



    streamingContext.start()

    streamingContext.awaitTermination()

  }

}

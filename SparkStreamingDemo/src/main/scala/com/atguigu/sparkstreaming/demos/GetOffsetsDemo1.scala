package com.atguigu.sparkstreaming.demos

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.TaskContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
 * Created by Smexy on 2022/4/23
 *
 *    at least once:
 *          ①取消自动提交offsets
 *          ②在输出后，手动提交
 *
 *
 *    把对DStream的计算转为对DStream中RDD的计算！
 *        DStream是RDD的无限集合! DStream中每个批次采集到的数据会封装为一个RDD，调用DStream的计算逻辑！
 *
 *     有哪些算子可以实现把对DStream的计算转为对DStream中RDD的计算?
 *
 *        foreachRDD:   foreachRDD(foreachFunc: RDD[T] => Unit): Unit
 *                        输出算子，用来将计算的结果输出到数据库！ 在计算的末尾使用!
 *
 *
 *
 *        transform:   transform[U: ClassTag](transformFunc: (RDD[T], Time) => RDD[U]): DStream[U]
 *                        在计算中间使用，可以拿到新返回的DS继续转换!
 *
 *                      在你需要时用!
 *                          DStream中已经自带了一些转换算子，例如map,filter....但是有一些算子，在RDD的API中有的，在DStream中没有
 *                          如果你希望用这些算子，只能调用tranform，把对DStream的计算转换为对其中RDD的计算，这样，你可以使用RDD中独有的
 *                          算子进行计算!
 *
 *
 *        ------------------------------------------
 * Exception in thread "main" java.lang.ClassCastException:
 *    org.apache.spark.rdd.MapPartitionsRDD
 *      cannot be cast to
 *    org.apache.spark.streaming.kafka010.HasOffsetRanges: 唯一实现 KafkaRDD
 *
 *
 *        val ranges: Array[OffsetRange] = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
 *        必须使用初始DStream调用foreachRDD或transform才能转换成功！
 *            只有KafkaUtils.createDirectStream 返回的DStream里面封装的才是KafkaRDD！
 *
 *
 *
 *
 */
object GetOffsetsDemo1 {

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




    val ds2: DStream[ConsumerRecord[String, String]] = stream.transform(rdd => {

      val ranges: Array[OffsetRange] = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

      for (elem <- ranges) {
        println(elem)
      }

      rdd

    })

    val ds3: DStream[String] = ds2.map(record => record.value())

    ds3.print(1000)



    /*stream.foreachRDD { rdd =>
      // 获取偏移量
      // 判断当前的rdd是否是HasOffsetRanges的子类，如果是就强转为HasOffsetRanges类型之后调用offsetRanges方法，返回偏移量
      // OffsetRange: 代表消费的主题的一个分区的元数据信息
      val ranges: Array[OffsetRange] = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

      for (elem <- ranges) {
        println(elem)
      }

    }*/


    streamingContext.start()

    streamingContext.awaitTermination()

  }

}

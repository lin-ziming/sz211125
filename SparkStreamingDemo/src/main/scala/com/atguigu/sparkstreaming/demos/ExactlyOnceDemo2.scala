package com.atguigu.sparkstreaming.demos

import java.sql.{Connection, PreparedStatement, ResultSet}

import com.atguigu.sparkstreaming.utils.JDBCUtil
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.{CanCommitOffsets, ConsumerStrategies, HasOffsetRanges, KafkaUtils, OffsetRange}

import scala.collection.mutable

/**
 * Created by Smexy on 2022/4/26
 *
 *    at least once + 事务输出(mysql)
 *
 *    以wordcount为例。
 *
 *    设计数据在mysql中存储的表。
 *        数据:   （word,count）
 *                    粒度： 一行是一个单词
 *
 *        offsets: (groupId,topic,partition,offset)
 *                      offset: 只记录 untilOffset
 *                    粒度： 一行是一个组消费一个主题一个分区的偏移量信息
 *
 *    -----------------------
 *
 * CREATE TABLE `wordcount` (
 * `word` varchar(100) NOT NULL,
 * `count` bigint(20) DEFAULT NULL,
 * PRIMARY KEY (`word`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8
 *
 * CREATE TABLE `offsets` (
 * `groupId` varchar(100) NOT NULL,
 * `topic` varchar(100) NOT NULL,
 * `partitionId` int(11) NOT NULL,
 * `untilOffset` bigint(20) DEFAULT NULL,
 * PRIMARY KEY (`groupId`,`topic`,`partitionId`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8
 *
 * -----------------------------
 *    at least once + 事务输出流程
 *
 *    ①查询之前在Mysql中已经提交的偏移量
 *    ②参考查询出的已经提交的偏移量，获取一个从此偏移量消费的DS
 *    ③获取DS中的偏移量
 *    ④你自己的计算
 *    ⑤获取计算的结果
 *    ⑥ 开启事务
 *        更新结果
 *        更新偏移量
 *       提交事务
 */
object ExactlyOnceDemo2 {

  val groupId="211125test4"

  val topicName="topicA"

  /*
      声明查询历史提交offsets的方法
   */
  def readHistoryOffsets(groupId:String,topicName:String):Map[TopicPartition, Long]={

    val result = new mutable.HashMap[TopicPartition, Long]()

    val sql=
      """
        |select
        |   partitionId,untilOffset
        |from offsets
        |where groupId=? and topic=?
        |
        |
        |""".stripMargin

    var connection: Connection = null
    var ps: PreparedStatement = null

    try {
      connection = JDBCUtil.getConnection()
      ps = connection.prepareStatement(sql)


      ps.setString(1, groupId);
      ps.setString(2, topicName);

      val resultSet: ResultSet = ps.executeQuery()
      while (resultSet.next()) {
        // 每迭代一次，放入一个分区的信息
        result.put(new TopicPartition(topicName, resultSet.getInt("partitionId")), resultSet.getLong("untilOffset"))

      }
    } catch {
      case e:Exception =>{
        e.printStackTrace()
      }
    } finally {

      if (ps != null){
        ps.close()
      }

      if (connection != null){

        connection.close()

      }
    }

    //可变转不可变
    result.toMap

  }

  /*
        有状态的计算(当前):   当前批次在计算时，需要使用上一个批次计算的结果
                            实现全局累加

        无状态的计算:   每个批次都是独立的，没有联系
                            只是当前批次求和
   */
  def writeDataAndOffsetsInCommonTransaction(result: Array[(String, Int)], ranges: Array[OffsetRange]) = {

    /*
          Insert into: 不能用
          insert update: 简单,如果主键存在就更新，在更新时，可以获取更新前的值
                             count: 当前count字段的值
                             values(count): 要写入的count字段的值

          replace into: 麻烦，需要先查之前的状态，相加之后，再写入
     */
    var sql1=
      """
        |
        |INSERT INTO wordcount
        |VALUES(?,?)
        |ON DUPLICATE KEY UPDATE count=values(count) + count
        |
        |
        |""".stripMargin

    var sql2=
      """
        |
        |replace INTO offsets VALUES(?,?,?,?)
        |
        |
        |
        |""".stripMargin

    var connection: Connection = null
    var ps1: PreparedStatement = null
    var ps2: PreparedStatement = null

    try {
      connection = JDBCUtil.getConnection()

      //取消事务的自动提交，改为手动提交
      connection.setAutoCommit(false)

      ps1 = connection.prepareStatement(sql1)
      ps2 = connection.prepareStatement(sql2)

      //更新数据
      for ((word, count) <- result) {

          ps1.setString(1,word)
           ps1.setLong(2,count)

          //攒起来
        ps1.addBatch()

      }

      //更新offsets
      for (offsetRange <- ranges) {

        ps2.setString(1,groupId)
        ps2.setString(2,topicName)
        ps2.setInt(3,offsetRange.partition)
        ps2.setLong(4,offsetRange.untilOffset)

        //攒起来
        ps2.addBatch()

      }
      
      //批量执行
      val dataRepsonse: Array[Int] = ps1.executeBatch()
      val offsetsRepsonse: Array[Int] = ps2.executeBatch()

      //提交事务
      connection.commit()

      println("数据写成功了:"+dataRepsonse.size)
      println("offsets写成功了:"+offsetsRepsonse.size)


    } catch {
      case e:Exception =>{
        //回滚事务
        connection.rollback()

        e.printStackTrace()
      }
    } finally {

      if (ps1 != null){
        ps1.close()
      }

      if (ps2 != null){
        ps2.close()
      }

      if (connection != null){

        connection.close()

      }
    }





  }

  def main(args: Array[String]): Unit = {

    val streamingContext = new StreamingContext("local[*]", "ExactlyOnceDemo2", Seconds(5))


    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "hadoop102:9092,hadoop103:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "sz211125",
      // latest从最近位置消费，earliest:如果当前组从来没有消费过这个主题，从主题的最开始位置消费
      //"auto.offset.reset" -> "latest",
      // ******是否允许自动提交offset
      "enable.auto.commit" -> "false"
    )

    val topics = Array(topicName)

    // 第一步: 先去mysql保存offsets的表中，查询上次记录的位置，从这个位置往后开始获取一个数据流
    // 根据groupId和topic查询上次消费的每一个分区的位置
    val offsetsMap: Map[TopicPartition, Long] = readHistoryOffsets(groupId, topicName)

    // 第二步： 基于查询出的偏移量获取一个从此偏移量往后的流
    /*
        Assign: 独立消费者。 偏移量信息和 kafka中维护的 _consumeroffsets没有任何关系。自己去维护。如果是一个新的消费者组，要在数据库中初始化它。

        Subscribe： 非独立消费者。偏移量可以参考 kafka中维护的 _consumeroffsets
     */
    import  ConsumerStrategies._

    val stream: InputDStream[ConsumerRecord[String, String]] = KafkaUtils.createDirectStream[String, String](
      streamingContext,
      PreferConsistent,
      Assign[String, String](offsetsMap.keys, kafkaParams, offsetsMap)
    )


    stream.foreachRDD { rdd =>

      if (!rdd.isEmpty()){

        // 获取偏移量  从之前维护offsets的存储中获取偏移量    offsets在Driver端
        val ranges: Array[OffsetRange] = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

        // 各种转换
        val resultRdd: RDD[(String, Int)] = rdd.flatMap(record => record.value().split(" "))
          .map(word => (word, 1))
          .reduceByKey(_ + _)

        // 将结果收集到driver端
        val result: Array[(String, Int)] = resultRdd.collect()

        // 保证事务输出  将结果和偏移量一起在一个事务中进行写出
        writeDataAndOffsetsInCommonTransaction(result,ranges)

      }

    }



    streamingContext.start()

    streamingContext.awaitTermination()


  }

}

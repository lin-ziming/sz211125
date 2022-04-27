package com.atguigu.gmall.realtime.apps

import java.sql.{Connection, PreparedStatement, ResultSet}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.alibaba.fastjson.JSON
import com.atguigu.gmall.constants.TopicConstant
import com.atguigu.gmall.realtime.apps.DAUApp.{appName, batchDuration, context}
import com.atguigu.gmall.realtime.beans.OrderInfo
import com.atguigu.gmall.realtime.utils.{JDBCUtil, MyKafkaUtil}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, OffsetRange}
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.collection.mutable

/**
 * Created by Smexy on 2022/4/27
 *
 *  聚合类应用，每天产生上亿的订单，最后我要的结果只有24行!
 *      日期  小时  GMV
 *      聚合的结果的数据量非常小，完全可以采用Mysql来存储结果!
 *
 *      at least once + 事务输出 保证消费的精确一次!
 *
 *      -------------------
 *
 * CREATE TABLE `gmvstats` (
 * `create_date` date NOT NULL,
 * `create_hour` varchar(2) NOT NULL,
 * `gmv` decimal(16,2) DEFAULT NULL,
 * PRIMARY KEY (`create_date`,`create_hour`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8
 */
object GMVApp extends  BaseApp {
  override var groupName: String = "sz1125"
  override var batchDuration: Int = 10
  override var appName: String = "GMVApp"

  // 提供方法查询当前组消费主题的历史offsets
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


  def writeDataAndOffsetsInCommonTransaction(result: Array[((String, String), Double)], ranges: Array[OffsetRange]) = {

    var sql1=
      """
        |
        |INSERT INTO gmvstats
        |VALUES(?,?,?)
        |ON DUPLICATE KEY UPDATE gmv=values(gmv) + gmv
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
      for (((date, hour), gmv) <- result) {

        ps1.setString(1,date)
        ps1.setString(2,hour)
        ps1.setDouble(3,gmv)

        //攒起来
        ps1.addBatch()

      }

      //更新offsets
      for (offsetRange <- ranges) {

        ps2.setString(1,groupName)
        ps2.setString(2,TopicConstant.GMALL_ORDER_INFO)
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


  def parseRecord(rdd: RDD[ConsumerRecord[String, String]]): RDD[OrderInfo] = {

    rdd.map(record => {

      val orderInfo: OrderInfo = JSON.parseObject(record.value(), classOf[OrderInfo])

      // 时间: create_time": "2022-04-27 03:18:39",
      val f1: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      val f2: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
      val f3: DateTimeFormatter = DateTimeFormatter.ofPattern("HH")

      val localDateTime: LocalDateTime = LocalDateTime.parse(orderInfo.create_time, f1)
      orderInfo.create_date=localDateTime.format(f2)
      orderInfo.create_hour=localDateTime.format(f3)

      orderInfo

    })


  }

  def main(args: Array[String]): Unit = {

    context = new StreamingContext("local[*]",appName,Seconds(batchDuration))

    //查询之前消费的Offsets
    val offsetsMap: Map[TopicPartition, Long] = readHistoryOffsets(groupName, TopicConstant.GMALL_ORDER_INFO)

    runApp{

      val ds: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream(Array(TopicConstant.GMALL_ORDER_INFO), context, groupName, true, offsetsMap)
      
      ds.foreachRDD(rdd => {
        
        if (!rdd.isEmpty()){

          //获取偏移量
          val ranges: Array[OffsetRange] = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

          //封装ConsumerRecord 为样例类
          val rdd1: RDD[OrderInfo] = parseRecord(rdd)

          //计算
          val result: Array[((String, String), Double)] = rdd1.map(orderInfo => ((orderInfo.create_date, orderInfo.create_hour), orderInfo.total_amount))
            .reduceByKey(_ + _)
            .collect()

          // 将计算的结果和offsets在一个事务中一起写入到Mysql
          writeDataAndOffsetsInCommonTransaction(result,ranges)

        }
        
      })




    }

  }
}

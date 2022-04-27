package com.atguigu.gmall.realtime.utils

import java.sql.Connection
import java.util.Properties

import com.alibaba.druid.pool.DruidDataSourceFactory
import javax.sql.DataSource

/**
 * Created by VULCAN on 2020/11/3
 */
object JDBCUtil {

    // 创建连接池对象
    var dataSource:DataSource = init()

    // 连接池的初始化
    def init():DataSource = {

        val paramMap = new java.util.HashMap[String, String]()

        val properties: Properties = PropertiesUtil.load("config.properties")

        paramMap.put("driverClassName", properties.getProperty("jdbc.driver.name"))
        paramMap.put("url", properties.getProperty("jdbc.url"))
        paramMap.put("username", properties.getProperty("jdbc.user"))
        paramMap.put("password", properties.getProperty("jdbc.password"))
        paramMap.put("maxActive", properties.getProperty("jdbc.datasource.size"))

        // 使用Druid连接池对象
        DruidDataSourceFactory.createDataSource(paramMap)
    }

    // 从连接池中获取连接对象
    def getConnection(): Connection = {
        dataSource.getConnection
    }

    def main(args: Array[String]): Unit = {

        println(getConnection())

    }
 
}

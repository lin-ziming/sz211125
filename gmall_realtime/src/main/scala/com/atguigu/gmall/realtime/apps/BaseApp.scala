package com.atguigu.gmall.realtime.apps

import org.apache.spark.streaming.StreamingContext

/**
 * Created by Smexy on 2022/4/24
 *
 *    StreamingContext: 由集群，Appname和采集周期构成。不同的需求可能需要灵活地设置这三个参数
 *
 *    拥有抽象属性的类，必须是抽象类
 */
abstract class BaseApp {

  var groupName:String

  //只声明不赋值，称为抽象属性
  var batchDuration:Int

  var appName:String

  // 子类在集成父类时，Overwrite
  var context:StreamingContext = null

  def runApp(code: => Unit) :Unit={
    try {
      //将②③④作为参数传入
      code
      context.start()
      context.awaitTermination()
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        throw new RuntimeException("运行出错!")
    }
  }


}

package com.atguigu.gmall.realtime

import com.alibaba.fastjson.JSON
import com.google.gson.Gson

/**
 * Created by Smexy on 2022/4/29
 *
 * Error:(14, 28) ambiguous reference to overloaded definition,
 * both method toJSONString in class JSON of type (x$1: Any, x$2: com.alibaba.fastjson.serializer.SerializerFeature*)String
 * and  method toJSONString in class JSON of type (x$1: Any)String
 * match argument types (com.atguigu.gmall.realtime.Cat) and expected result type String
 * val str: String = JSON.toJSONString(cat)
 */
object JsonParsedemo {

  def main(args: Array[String]): Unit = {

    val cat: Cat = Cat("miaomiao")

    //val str: String = JSON.toJSONString(cat)

    val str: String = new Gson().toJson(cat)

    println(str)

  }

}

case class  Cat(name:String)

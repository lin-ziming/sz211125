package com.atguigu.gmall.realtime.beans

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class CouponAlertInfo(aid:String,
                           uids:mutable.Set[String],
                           itemIds:mutable.Set[String],
                           events:ListBuffer[String],
                           ts:Long)
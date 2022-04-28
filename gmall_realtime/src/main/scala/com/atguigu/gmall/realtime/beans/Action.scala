package com.atguigu.gmall.realtime.beans

case class Action(action_id:String,
                  item:String,
                  item_type:String,
                  ts:Long
                 )
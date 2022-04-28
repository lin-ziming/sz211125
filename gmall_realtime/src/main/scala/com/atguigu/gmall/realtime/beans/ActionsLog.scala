package com.atguigu.gmall.realtime.beans

case class ActionsLog(
                       actions: List[Action],
                       ts:Long,
                       common:CommonInfo
                     )
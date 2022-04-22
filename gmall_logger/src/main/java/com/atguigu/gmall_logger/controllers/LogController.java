package com.atguigu.gmall_logger.controllers;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.constants.TopicConstant;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by Smexy on 2022/4/22
 *
 *      发送的参数名固定叫param
 *
 *      http://localhost:8888/gmall_logger/log?param={json日志}
 *
 *      -------------
 *          启动kafka之前，修改 server.properties ，添加 num.partitions=3
 */
@RestController
@Log4j // Logger log = Logger.getLogger(LogController.class);
public class LogController {

    @Autowired
    private KafkaTemplate client;

    @RequestMapping(value = "/hello")
    public Object handle2(){

        System.out.println("hello");

        return "success！";


    }

    @RequestMapping(value = "/log")
    public Object handle1(String param){

        //记录日志到文件
        log.info(param);

        //按照日志类型，发送到kafka的不同主题
        JSONObject jsonObject = JSON.parseObject(param);

        if (jsonObject.containsKey("start")){

            client.send(TopicConstant.STARTUP_LOG,param);

        }

        if (jsonObject.containsKey("page")){

            client.send(TopicConstant.PAGE_LOG,param);

        }

        if (jsonObject.containsKey("actions")){

            client.send(TopicConstant.ACTIONS_LOG,param);

        }

        if (jsonObject.containsKey("displays")){

            client.send(TopicConstant.DISPLAY_LOG,param);

        }

        if (jsonObject.containsKey("err")){

            client.send(TopicConstant.ERROR_LOG,param);

        }

        return "receive success！";


    }
}

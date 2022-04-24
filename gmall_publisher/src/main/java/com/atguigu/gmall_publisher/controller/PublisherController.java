package com.atguigu.gmall_publisher.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall_publisher.bean.DAUData;
import com.atguigu.gmall_publisher.service.PublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Smexy on 2022/4/24
 */
@RestController
public class PublisherController {

    @Autowired
    private PublisherService publisherService;

    /*
         http://localhost:8070/  realtime-total ?date=2021-08-15

         [
            {"id":"dau","name":"当日日活数","value":1200},
            {"id":"new_mid","name":"新增设备数","value":233}
         ]

         []: List 或 JSONArray
         {}: Map 或 JSONObject

         List [
                JSONObject1，
                JSONObject2

                ]
     */
    @RequestMapping(value = "/realtime-total")
    public Object handle1(String date){

        Integer dauByDate = publisherService.getDAUByDate(date);
        Integer count = publisherService.getNewMidCount(date);

        JSONObject jo1 = new JSONObject();
        jo1.put("id","dau");
        jo1.put("name","当日日活数");
        jo1.put("value",dauByDate);


        JSONObject jo2 = new JSONObject();

        jo2.put("id","new_mid");
        jo2.put("name","新增设备数");
        jo2.put("value",count);

        ArrayList<JSONObject> result = new ArrayList<>();

        result.add(jo1);
        result.add(jo2);

        return result;

    }

    /*
        http://localhost:8070/   realtime-hours  ?id=dau&  date=2021-08-15

        {
        "yesterday":{"11":383,"12":123,"17":88,"19":200 },
            "today":{"12":38,"13":1233,"17":123,"19":688 }
        }

        Map|JSONObject
     */
    @RequestMapping(value = "/realtime-hours")
    public Object handle2(String id,String date){

        //基于今天日期求昨天日期
        LocalDate localDate = LocalDate.parse(date);

        String yesterStr = localDate.minusDays(1).toString();

        List<DAUData> todayData = publisherService.getDAUDataByDate(date);
        List<DAUData> yestodayData = publisherService.getDAUDataByDate(yesterStr);


        JSONObject result = new JSONObject();

        result.put("yesterday",parseDAUData(yestodayData));
        result.put("today",parseDAUData(todayData));

        return result;

    }

    // 把一个 List<DAUData> 转为一个Map 或 JSONObject
    public JSONObject parseDAUData(List<DAUData> data){

        JSONObject result = new JSONObject();

        for (DAUData d : data) {

            result.put(d.getHour(),d.getCount());

        }

        return result;

    }
}

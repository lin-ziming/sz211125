package com.atguigu.gmall_publisher.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall_publisher.bean.DAUData;
import com.atguigu.gmall_publisher.bean.GMVData;

import java.io.IOException;
import java.util.List;

/**
 * Created by Smexy on 2022/4/24
 */
public interface PublisherService {

    // http://localhost:8070/realtime-total?date=2021-08-15
    // 查询当日日活
    Integer getDAUByDate(String date);

    //查询当日新增的设备数
    Integer getNewMidCount(String date);

    // http://localhost:8070/realtime-hours?id=dau&date=2021-08-15
    // 查询一天中各小时活跃的设备
    List<DAUData> getDAUDataByDate(String date);

    // 查询当日GMV
    Double getGMVByDate(String date);

    // 查询一天中各小时GMV
    List<GMVData> getGMVDataByDate(String date);

    JSONObject getESData(String date111, Integer startpage, Integer size , String keyword) throws IOException;

}

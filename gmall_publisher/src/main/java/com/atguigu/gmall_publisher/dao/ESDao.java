package com.atguigu.gmall_publisher.dao;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;

/**
 * Created by Smexy on 2022/4/29
 */
public interface ESDao {

    // http://localhost:8070/sale_detail?   date=2021-08-21 &startpage=1  &size=5& keyword=小米手机
    JSONObject getESData(String date111,Integer startpage,Integer size ,String keyword) throws IOException;

}

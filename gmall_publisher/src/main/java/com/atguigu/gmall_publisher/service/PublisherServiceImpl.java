package com.atguigu.gmall_publisher.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall_publisher.bean.DAUData;
import com.atguigu.gmall_publisher.bean.GMVData;
import com.atguigu.gmall_publisher.dao.ESDao;
import com.atguigu.gmall_publisher.mapper.DAUMapper;
import com.atguigu.gmall_publisher.mapper.GMVMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Created by Smexy on 2022/4/24
 */
@Service
public class PublisherServiceImpl implements  PublisherService {

    @Autowired
    private DAUMapper dauMapper;

    @Override
    public Integer getDAUByDate(String date) {

        System.out.println("查询之前...");

        Integer dauByDate = dauMapper.getDAUByDate(date);

        System.out.println("查询之后...");

        return dauByDate;
    }

    @Override
    public Integer getNewMidCount(String date) {

        System.out.println("查询之前...");

        Integer count = dauMapper.getNewMidCount(date);

        System.out.println("查询之后...");

        return count;

    }

    @Override
    public List<DAUData> getDAUDataByDate(String date) {

        System.out.println("查询之前...");

        List<DAUData> dauDataByDate = dauMapper.getDAUDataByDate(date);

        System.out.println("查询之后...");

        return dauDataByDate;
    }
    @Autowired
    private GMVMapper gmvMapper;

    @Override
    public Double getGMVByDate(String date) {

        System.out.println("查询之前...");

        Double gmvByDate = gmvMapper.getGMVByDate(date);

        System.out.println("查询之后...");

        return gmvByDate;
    }

    @Override
    public List<GMVData> getGMVDataByDate(String date) {
        System.out.println("查询之前...");

        List<GMVData> gmvDataByDate = gmvMapper.getGMVDataByDate(date);

        System.out.println("查询之后...");

        return gmvDataByDate;
    }

    @Autowired
    private ESDao esDao;

    @Override
    public JSONObject getESData(String date111, Integer startpage, Integer size, String keyword) throws IOException {

        System.out.println("查询之前...");

        JSONObject jsonObject = esDao.getESData(date111, startpage, size, keyword);

        System.out.println("查询之后...");

        return jsonObject;
    }
}

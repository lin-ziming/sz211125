package com.atguigu.gmall_publisher.service;

import com.atguigu.gmall_publisher.bean.DAUData;
import com.atguigu.gmall_publisher.mapper.DAUMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}

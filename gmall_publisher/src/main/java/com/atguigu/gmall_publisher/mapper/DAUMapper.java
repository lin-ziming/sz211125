package com.atguigu.gmall_publisher.mapper;

import com.atguigu.gmall_publisher.bean.DAUData;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by Smexy on 2022/4/24
 */
@Repository
@DS("hbase")
public interface DAUMapper {

    // http://localhost:8070/realtime-total?date=2021-08-15
    // 查询当日日活
    Integer getDAUByDate(String date);

    //查询当日新增的设备数
    Integer getNewMidCount(String date);

    // http://localhost:8070/realtime-hours?id=dau&date=2021-08-15
    // 查询一天中各小时活跃的设备
    List<DAUData> getDAUDataByDate(String date);


}

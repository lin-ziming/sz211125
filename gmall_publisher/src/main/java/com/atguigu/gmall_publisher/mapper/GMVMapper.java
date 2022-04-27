package com.atguigu.gmall_publisher.mapper;

import com.atguigu.gmall_publisher.bean.DAUData;
import com.atguigu.gmall_publisher.bean.GMVData;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by Smexy on 2022/4/24
 */
@Repository
@DS("mysql")
public interface GMVMapper {


    // 查询当日GMV
    Double getGMVByDate(String date);

    // 查询一天中各小时GMV
    List<GMVData> getGMVDataByDate(String date);


}

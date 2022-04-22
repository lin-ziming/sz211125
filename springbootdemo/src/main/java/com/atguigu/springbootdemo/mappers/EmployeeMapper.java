package com.atguigu.springbootdemo.mappers;


import com.atguigu.springbootdemo.beans.Employee;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by Smexy on 2022/4/19
 *
 *      mapper:  指编写了sql的xml文件
 *      Mapper:  Dao
 *
 *      接口无法实例化！必须为它提供实现类，为实现类提供实例!
 *
 *      在Mybatis中，无需按照以上步骤操作，Mybatis提供了动态代理(无需编写实现类，Mybatis可以自动为接口实例化一个对象)机制
 */
@Repository// 如果标注在了接口上，这个接口式Mapper，需要告诉容器，请用Mybatis的动态代理机制为它创建实例
public interface EmployeeMapper {


    //增删改查
    Employee getEmployeeById(Integer id);

    void insertEmployee(Employee employee);

    void updateEmployee(Employee employee);

    void deleteEmployeeById(Integer id);

    // 查询所有
    List<Employee> getAll();


}

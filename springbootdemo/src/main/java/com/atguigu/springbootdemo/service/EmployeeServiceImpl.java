package com.atguigu.springbootdemo.service;

import com.atguigu.springbootdemo.beans.Employee;
import com.atguigu.springbootdemo.mappers.EmployeeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Smexy on 2022/4/22
 */
// new EmployeeServiceImpl()
    @Service
public class EmployeeServiceImpl implements EmployeeService {

    //声明Dao
    @Autowired
    private EmployeeMapper employeeMapper;

    @Override
    public Employee getEmployeeById(Integer id) {

        System.out.println("操作之前do something....");

        Employee employee = employeeMapper.getEmployeeById(id);

        System.out.println("操作之后do something....");

        return employee;
    }

    @Override
    public void insertEmployee(Employee employee) {

        System.out.println("操作之前do something....");

        employeeMapper.insertEmployee(employee);

        System.out.println("操作之后do something....");

    }

    @Override
    public void updateEmployee(Employee employee) {

        System.out.println("操作之前do something....");

        employeeMapper.updateEmployee(employee);

        System.out.println("操作之后do something....");

    }

    @Override
    public void deleteEmployeeById(Integer id) {

        System.out.println("操作之前do something....");

        employeeMapper.deleteEmployeeById(id);

        System.out.println("操作之后do something....");

    }

    @Override
    public List<Employee> getAll() {

        System.out.println("操作之前do something....");

        List<Employee> all = employeeMapper.getAll();

        System.out.println("操作之后do something....");

        return all;
    }
}

package com.atguigu.springbootdemo.controllers;

import com.atguigu.springbootdemo.beans.Employee;
import com.atguigu.springbootdemo.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by Smexy on 2022/4/22
 */
@RestController
public class EmployeeController {

    // employeeService= new EmployeeServiceImpl()
    @Autowired // =  自动从容器中找标注了次注解 的类型的对象，找到就赋值
    private EmployeeService employeeService;

    @RequestMapping(value = "/emp")
    public Object handle1(String op,Integer id,String lastname,String gender,String email){

        //封装数据模型
        Employee employee = new Employee(id, lastname, gender, email);

        switch (op){

            case "select": if (id == null){
                return "必须传入员工id！";
            }else {
                Employee e = employeeService.getEmployeeById(id);
                return e == null ? "查无此人!" : e;
            }

            case "insert" : employeeService.insertEmployee(employee);
                    return "操作完成!";

            case "update": if (id == null){
                return "必须传入员工id！";
            }else {
                 employeeService.updateEmployee(employee);
                return  "操作完成!";
            }

            case "delete": if (id == null){
                return "必须传入员工id！";
            }else {
                employeeService.deleteEmployeeById(id);
                return  "操作完成!";
            }

            default: return "请正确操作";


        }



    }

    @RequestMapping(value = "/getAllEmp")
    public Object handle2(){

        List<Employee> all = employeeService.getAll();

        return all;

    }


}

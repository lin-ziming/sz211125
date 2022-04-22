package com.atguigu.log4j.demos;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.log4j.beans.Employee;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Smexy on 2022/4/22
 *
 *      JSON:   核心类
 *          string 转  java对象
 *              {}:  JSONObject  Map
 *                          JSON.parseObject()
 *              []:  JSONArray   List
 *                          JSON.parseArray()
 *
 *          java对象 转 json string:  JSON.toJSONString()
 *
 */
public class JSONHandle {

    public static void main(String[] args) {

        Employee employee2 = new Employee(1, "jack", "aaa", "bbb");
        Employee employee1 = new Employee(1, "jack1", "aaa", "bbb");

        ArrayList<Employee> employees = new ArrayList<>();

        employees.add(employee1);
        employees.add(employee2);

        //{"email":"bbb","gender":"aaa","id":1,"lastName":"jack"}
        String str1 = JSON.toJSONString(employee2);
        //[{"email":"bbb","gender":"aaa","id":1,"lastName":"jack1"},{"email":"bbb","gender":"aaa","id":1,"lastName":"jack"}]
        String str2 = JSON.toJSONString(employees);

        System.out.println(str1);
        System.out.println(str2);

        System.out.println("---------------------str 转 对象------------------");

        JSONObject jsonObject = JSON.parseObject(str1);

        System.out.println(jsonObject.getString("email"));

        JSONArray jsonArray = JSON.parseArray(str2);

        System.out.println(jsonArray.getObject(1, Employee.class));

        System.out.println("---------------------str 转 对象2------------------");

        Employee employee = JSON.parseObject(str1, Employee.class);

        System.out.println(employee);

        List<Employee> employees1 = JSON.parseArray(str2, Employee.class);

        System.out.println(employees1);

        System.out.println("---------------------Gson------------------");

        Gson gson = new Gson();

        System.out.println(gson.toJson(employees));
        System.out.println(gson.toJson(employee1));

        // gson 把字符串转对象，比较麻烦，这里不介绍


    }


}

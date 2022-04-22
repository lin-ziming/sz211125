package com.atguigu.log4j.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Smexy on 2022/4/19
 *
 *  标准的JavaBean:
 *          ①有空参构造器
 *          ②为私有属性提供公共的getter,setter
 */
@Data // 为私有属性生成getter,setter，提供toString
@AllArgsConstructor
@NoArgsConstructor
public class Employee {

    //不能用基本数据类型，必须用包装类
    // 因为基本数据类型有默认值
    private Integer id;
    private String lastName;
    private String gender;
    private String email;

    /*public void setLast_name(String col){

        this.lastName = col;

    }*/

    public static void main(String[] args) {

        //全参构造器，参数顺序和属性声明的顺序一致
        Employee employee = new Employee(1, "Tom", "male", "abc");

        Employee employee1 = new Employee();




    }


}

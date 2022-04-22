package com.atguigu.springbootdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//告诉容器，请用Mybatis的动态代理机制为它创建实例
@MapperScan(basePackages = "com.atguigu.springbootdemo.mappers")
public class SpringbootdemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootdemoApplication.class, args);
    }

}

package com.atguigu.springbootdemo.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by Smexy on 2022/4/22
 *      容器默认只扫描  SpringbootdemoApplication(主启动类)所在的包，及其子包!
 *
 *      @Controller的作用:
 *              ①@Controller给你自己看的，看了之后就知道顶了这个注解的类是一个控制器
 *              ②给容器看，当容器扫描到当前包时，发现有个类顶了这个注解，会自动为这个类在容器中创建一个单例对象
 */
@Controller
public class HelloController {


    /*
            <a href="hello">

            @RequestMapping: 标注在方法上面，表明当前方法是用来处理哪个请求!
     */
    @RequestMapping(value = "/hello")
    public Object handle1(){

        System.out.println("处理了hello请求，即将返回页面");

        return "/a.html";

    }


}

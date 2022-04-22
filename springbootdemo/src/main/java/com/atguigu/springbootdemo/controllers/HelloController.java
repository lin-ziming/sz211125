package com.atguigu.springbootdemo.controllers;

import com.atguigu.springbootdemo.beans.Employee;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by Smexy on 2022/4/22
 *      容器默认只扫描  SpringbootdemoApplication(主启动类)所在的包，及其子包!
 *
 *      @Controller的作用:
 *              ①@Controller给你自己看的，看了之后就知道顶了这个注解的类是一个控制器
 *              ②给容器看，当容器扫描到当前包时，发现有个类顶了这个注解，会自动为这个类在容器中创建一个单例对象
 */
//@Controller
@RestController//  @Controller + 为当前标注了次注解的类的所有方法自动添加    @ResponseBody
public class HelloController {


    /*
            http://localhost:8081/springbootdemo/hello

            @RequestMapping: 标注在方法上面，表明当前方法是用来处理哪个请求!
     */
    //@ResponseBody
    @RequestMapping(value = "/hello")
    public Object handle1(){

        System.out.println("处理了hello请求，即将返回页面");

        return "/a.html";

    }


    /*
        返回数据的类型:
                字面量:       从字面上就知道变量的值是什么的变量
                                int a = 100
                                String b = "abc"

                                返回数据时，直接将字面量返回，不做任何处理!

                非字面量:    一般是我们封装的对象
                                    Employee e = new Employee()

                                返回数据之前，调用jackson(spring自带的一个json框架)把对象转为json字符串，再返回
     */
    //@ResponseBody
    @RequestMapping(value = "/getData1")
    public Object handle2(){

        System.out.println("处理了getData1请求，即将返回字面量");

        return "/a.html";

    }

    //@ResponseBody
    @RequestMapping(value = "/getData2")
    public Object handle3(){

        System.out.println("处理了getData2请求，即将返回非字面量");

        Employee employee = new Employee(1, "Tom", "male", "abc");

        return employee;

    }

    /*
        /springbootdemo/sendParam      ?name=jack&age=30

        只需要在处理方法的参数列表中声明和url中参数名名称一样的形参即可!
     */
    @RequestMapping(value = "/sendParam")
    public Object handle4( String username ,Integer age){

        System.out.println("处理了sendParam请求，收到了name："+ username +",age:"+age);

        return "参数收到!";

    }


}

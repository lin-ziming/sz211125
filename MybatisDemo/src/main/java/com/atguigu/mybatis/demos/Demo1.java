package com.atguigu.mybatis.demos;

import com.atguigu.mybatis.beans.Employee;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Smexy on 2022/4/19
 *
 *  SqlSessionFactory:  SqlSession工厂。作用是创建SqlSession。
 *
 *      SqlSession:  sql的会话(一旦建立连接，可以发送和返回无数次sql和结果)
 *      Connection:   和数据库的一次连接
 *
 *
 *          特斯拉工厂: 生成特斯拉
 *
 *
 *   --------------------------------------
 *      创建对象的方式（设计模式）:
 *             new 类的构造器()
 *
 *         建造者模式:   向要一个A对象，先创建一个专门用来建造A对象的对象
 *                            A a =  new  ABilder().build()
 *
 *          工厂模式:   想要一个B对象，造一个用来创建B的工厂
 *                            B b =  new BFactory().getB()
 *
 *     -------------------------------
 *      准备工作：
 *                  ①准备表
 *                  ②根据ORM映射，对表创建一个bean
 *
 *       开发:
 *                  ①导包
 *                          mybatis,mysql驱动,lombok
 *                  ②编写全局配置xml
 *                      使用提供的示例代码，创建SqlSessionFactory
 *
 *                  ③SqlSessionFactory.openSession()获取SqlSession
 *
 *                  ④编写mapper.xml（sql）
 *
 *                  ⑤将mapper在全局配置xml中<mappers>注册
 *                   在代码中调用  SqlSession对象的方法，传入要执行的sqlid和参数
 *
 *
 *    -----------------------------------
 *      当前方式的弊端:
 *                  ①调用的是Sqlsession内置的方法，不够灵活!
 *                  ②调用的是Sqlsession内置的方法，对参数的类型是没有检查的
 *                          容器出现参数类型不匹配导致的错误
 *                  ③返回值类型是Object，需要强转为自己需要的类型
 *
 *       解决:  用自己声明的方法进行增删改查! 接口式编程!
 *
 */
public class Demo1 {

    public static void main(String[] args) throws IOException {

        String resource = "kaixin.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        SqlSession sqlSession = sqlSessionFactory.openSession();

       // System.out.println(sqlSession);

        /*
            T selectOne(String statement, Object parameter);

            String statement： sql语句的唯一标识   namespace.id
            Object parameter: 向sql中传入的参数
         */
        Object o = sqlSession.selectOne("feichangbang.sql1", sqlSessionFactory);

        Employee employee = (Employee) o;

        /*
                id  last_name  gender  email
                ------  ---------  ------  -------------
                     1  Tom        male    Tom@163.com

                 Employee e =  new  com.atguigu.mybatis.beans.Employee()

                 如果查询出的mysql的列名是xxx,就调用 对象.setXxx()
                 e.setId(1);
                 e.setLast_name('Tom')
                 e.setGender('male')
                 e.setEmail('Tom@163.com')

                 原因： 查询出的列名和对象的setXxx()方法对不上

                 解决：  ①给提供对应的setXxx()
                        ②使用别名的方式，把last_name 起别名 lastName
                        ③原理和②一样，让Mybatis自动进行

         */

        System.out.println(employee);
    }
}

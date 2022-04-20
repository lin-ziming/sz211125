package com.atguigu.mybatis.mappers;

import com.atguigu.mybatis.beans.Employee;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Smexy on 2022/4/19
 *
 *      SqlSession只能在方法范围声明!
 *
 *      查询无需提交事务！
 *      增删改必须提交事务！
 *
 *    -------------常见错误-----------------
 *
 *    org.apache.ibatis.binding.BindingException:
 *        Type interface com.atguigu.mybatis.mappers.EmployeeMapper is not known to the MapperRegistry.
 *              EmployeeMapper 想调用的sql的mapper文件在已经注册的sql文件中找不到
 *
 *
 */
public class EmployeeMapperTest {

    private SqlSessionFactory sqlSessionFactory;

    {
        String resource = "kaixin.xml";
        InputStream inputStream = null;
        try {
            inputStream = Resources.getResourceAsStream(resource);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    }

   // private SqlSession session=sqlSessionFactory.openSession();

    public void template() {

        SqlSession session = sqlSessionFactory.openSession();
        try {
            // com.sun.proxy.$Proxy5 implements   com.atguigu.mybatis.mappers.EmployeeMapper
            EmployeeMapper mapper = session.getMapper(EmployeeMapper.class);

            //class com.sun.proxy.$Proxy5
            System.out.println(mapper.getClass());

            Class<?>[] interfaces = mapper.getClass().getInterfaces();

            for (Class<?> anInterface : interfaces) {
                // com.atguigu.mybatis.mappers.EmployeeMapper
                System.out.println(anInterface.getName());
            }
            // do work
        } finally {
            session.close();
        }
    }

    @org.junit.Test
    public void getEmployeeById() {

        SqlSession session = sqlSessionFactory.openSession();
        try {

            EmployeeMapper mapper = session.getMapper(EmployeeMapper.class);

            Employee employee = mapper.getEmployeeById(1);

            System.out.println(employee);


        } finally {
            session.close();
        }
    }

    @org.junit.Test
    public void insertEmployee() {

        Employee employee = new Employee(null, "Tom", "male", "abc");

        //自动提交
        SqlSession session = sqlSessionFactory.openSession(true);
        try {

            EmployeeMapper mapper = session.getMapper(EmployeeMapper.class);

            mapper.insertEmployee(employee);

        } finally {
            session.close();
        }

    }

    @org.junit.Test
    public void updateEmployee() {

        //自动提交
        SqlSession session = sqlSessionFactory.openSession(true);
        try {

            EmployeeMapper mapper = session.getMapper(EmployeeMapper.class);

            Employee employee = mapper.getEmployeeById(3);

            employee.setLastName("jackma");

            mapper.updateEmployee(employee);


        } finally {
            session.close();
        }
    }

    @org.junit.Test
    public void deleteEmployeeById() {

        //自动提交
        SqlSession session = sqlSessionFactory.openSession(true);
        try {

            EmployeeMapper mapper = session.getMapper(EmployeeMapper.class);

            mapper.deleteEmployeeById(2);

            //手动提交
            //session.commit();



        } finally {
            session.close();
        }
    }

    @org.junit.Test
    public void getAll() {

        SqlSession session = sqlSessionFactory.openSession();
        try {

            EmployeeMapper mapper = session.getMapper(EmployeeMapper.class);

            List<Employee> employees = mapper.getAll();

            System.out.println(employees);


        } finally {
            session.close();
        }
    }
}
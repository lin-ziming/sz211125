package com.atguigu.es.demos;

import com.atguigu.es.beans.Emp;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;

import java.io.IOException;

/**
 * Created by Smexy on 2022/4/29
 *
 *
 *      ③准备操作命令Action
 *
 *              读:  Search
 *              写:  Index(insert和update的合体，存在就更新，不存在就插入)
 *                   Delete
 *
 *              批量写: Bulk
 *
 *            所有的Action实现都提供了建造者模式。
 *                  获取Search，不是new Search()，而是 new SearchBulider().build()
 *
 */
public class WriteDemo3 {

    public static void main(String[] args) throws IOException {

        JestClientFactory jestClientFactory = new JestClientFactory();

        //设置集群地址
        jestClientFactory.setHttpClientConfig((new HttpClientConfig.Builder("http://hadoop102:9200")).build());

        //创建一个客户端,连接服务端
        JestClient jestClient = jestClientFactory.getObject();


        Emp emp = new Emp(1001l, 20, 222.22, "jack", "男", "喝酒");
        //准备命令 Action
        Index index = new Index.Builder(emp).index("test").type("emps").id("21").build();


        DocumentResult result = jestClient.execute(index);

        // 2xx: ok  4xx: 客户端错误  5xx:服务器端代码错误
        System.out.println(result.getResponseCode());

        //关闭连接
        jestClient.close();




    }
}

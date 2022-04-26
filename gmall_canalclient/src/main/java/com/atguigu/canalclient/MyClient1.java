package com.atguigu.canalclient;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Created by Smexy on 2022/4/26
 *
 * ①先创建一个客户端对象CanalConnector
 *
 * ②使用客户端对象连接 Canal server端
 *
 * ③订阅表
 *
 * ④解析订阅到的数据
 *
 * ⑤将数据写入kafka
 */
public class MyClient1 {

    public static void main(String[] args) throws InterruptedException, InvalidProtocolBufferException {

        /*
                ①先创建一个客户端对象CanalConnector

                SocketAddress address: canal server的主机名和端口号。 参考canal.properties 中的
                                        canal.ip = hadoop103
                                        canal.port = 11111

                String destination:  参考canal.properties 中的 canal.destinations = example
                                                可以写canal.destinations中的一个或N个，代表要连接的Mysql实例配置文件instance.properties
                                                所在的目录名

                String username:  参考 instance.properties中的canal.user
                                                当前版本没有，使用null

                String password:  参考 instance.properties中的canal.passwd
                                                      当前版本没有，使用null
         */
        CanalConnector canalConnector = CanalConnectors.newSingleConnector(new InetSocketAddress("hadoop103", 11111), "example", null, null);


        //②使用客户端对象连接 Canal server端
        canalConnector.connect();


        // ③订阅表   格式: 库名.表名
        canalConnector.subscribe("1125.order_info");


        // ④尝试拉取(消费)canal server已经骗到的数据
        while (true){

            Message message = canalConnector.get(100);

            if (message.getId() == -1){

                System.out.println("当前没有新数据产生，歇5s再干活....");
                //现在没有数据产生了，歇会
                Thread.sleep(5000);

                //继续去拉 ，开始下次循环
                continue;

            }

            //当拉取到数据时，进行解析 在当前的需求中只要  order_info表的insert
           // System.out.println(message);
            List<CanalEntry.Entry> entries = message.getEntries();

            for (CanalEntry.Entry entry : entries) {

                //表名
                //String tableName = entry.getHeader().getTableName();

                //对order_info表可能有多种操作，例如开关事务，也算写操作，只要insert
                CanalEntry.EntryType entryType = entry.getEntryType();

                if (entryType.equals(CanalEntry.EntryType.ROWDATA)){

                    //进行解析
                    parseData(entry.getStoreValue());

                }

            }


        }


        // ⑤解析订阅到的数据




    }

    private static void parseData(ByteString storeValue) throws InvalidProtocolBufferException {

        //反序列化为RowChange  代表一个sql反序列化后的 N行变化
        CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(storeValue);

        // 只要insert类型   EventType:sql最具体的类似，操作的关键字
        if (rowChange.getEventType() == CanalEntry.EventType.INSERT){

            List<CanalEntry.RowData> rowDatasList = rowChange.getRowDatasList();

            // 一个RowData代表一行
            for (CanalEntry.RowData rowData : rowDatasList) {

                JSONObject jsonObject = new JSONObject();

                //获取一行中insert后的所有列
                List<CanalEntry.Column> afterColumnsList = rowData.getAfterColumnsList();

                for (CanalEntry.Column column : afterColumnsList) {

                    jsonObject.put(column.getName() , column.getValue());

                }

                System.out.println(jsonObject);

            }

        }




    }
}

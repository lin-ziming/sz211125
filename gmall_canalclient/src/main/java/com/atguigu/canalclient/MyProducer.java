package com.atguigu.canalclient;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

/**
 * Created by Smexy on 2022/4/27
 */
public class MyProducer {

    private static  Producer<String,String> producer;

    //在静态代码块中为静态属性赋值
    static {

        producer = getProducer();

    }

    //返回一个生产者
    public  static Producer<String,String> getProducer(){

        //提供生产者必须的参数  ProducerConfig
        Properties properties = new Properties();

        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"hadoop102:9092,hadoop103:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");

        return new KafkaProducer<String,String>(properties);

    }

    //发送数据到kafka
    public  static  void  sendData(String topic,String data){

        producer.send(new ProducerRecord<String,String>(topic,data));

    }
}

package com.atguigu.log4j.demos;

import org.apache.log4j.Logger;

/**
 * Created by Smexy on 2022/4/19
 */
public class Log4jDemo1 {

    public static void main(String[] args) {

        //声明一个Logger
        Logger logger = Logger.getLogger(Log4jDemo1.class);

        //级别打印日志
        logger.trace("trace:haha");
        logger.debug("debug:haha");
        logger.info("info:haha");
        logger.warn("warn:haha");
        logger.error("error:haha");
        logger.fatal("fatal:haha");

        //做了修改

    }
}

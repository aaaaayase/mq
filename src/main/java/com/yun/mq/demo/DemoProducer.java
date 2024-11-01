package com.yun.mq.demo;

import com.yun.mq.mqclient.Channel;
import com.yun.mq.mqclient.Connection;
import com.yun.mq.mqclient.ConnectionFactory;
import com.yun.mq.mqserver.core.ExchangeType;

import java.io.IOException;

/**
 * @author yun
 * @date 2024/11/1 22:33
 * @desciption: 生产者 一般就是服务器程序
 */
public class DemoProducer {

    public static void main(String[] args) throws IOException, InterruptedException {

        System.out.println("启动生产者！");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setPort(9090);
        connectionFactory.setHost("127.0.0.1");

        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();

        // 创建交换机和队列
        channel.exchangeDeclare("testExchange", ExchangeType.DIRECT, true, false, null);
        channel.queueDeclare("testQueue", true, false, false, null);

        // 发布消息
        channel.basicPublish("testExchange", "testQueue", null, "d".getBytes());
        System.out.println("消息投递完成！！！");

        Thread.sleep(500);
        channel.close();
        connection.close();
    }
}

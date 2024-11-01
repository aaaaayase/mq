package com.yun.mq.demo;

import com.yun.mq.common.BinaryTool;
import com.yun.mq.common.Consumer;
import com.yun.mq.common.MqException;
import com.yun.mq.mqclient.Channel;
import com.yun.mq.mqclient.Connection;
import com.yun.mq.mqclient.ConnectionFactory;
import com.yun.mq.mqserver.core.BasicProperties;
import com.yun.mq.mqserver.core.ExchangeType;

import java.io.IOException;

/**
 * @author yun
 * @date 2024/11/1 22:31
 * @desciption: 消费者
 */
public class DemoConsumer {

    public static void main(String[] args) throws IOException, InterruptedException, MqException {
        System.out.println("启动消费者！");

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("127.0.0.1");
        connectionFactory.setPort(9090);

        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare("testExchange", ExchangeType.DIRECT, true, false, null);
        channel.queueDeclare("testQueue", true, false, false, null);

       channel.basicConsume("testQueue", true, new Consumer() {
           @Override
           public void handleDelivery(String consumerTag, BasicProperties basicProperties, byte[] body) throws IOException, MqException {
               System.out.println("消费开始！");

                   System.out.println("body="+ new String(body));

               System.out.println("消费结束！");

           }
       });

       // 由于不知道生产者生产多少消息 所以让消费者一直消费下去
        while (true) {
            Thread.sleep(500);
        }


    }


}

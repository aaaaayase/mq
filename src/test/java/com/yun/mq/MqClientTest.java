package com.yun.mq;

import com.yun.mq.common.Consumer;
import com.yun.mq.common.MqException;
import com.yun.mq.mqclient.Channel;
import com.yun.mq.mqclient.Connection;
import com.yun.mq.mqclient.ConnectionFactory;
import com.yun.mq.mqserver.BrokerServer;
import com.yun.mq.mqserver.core.BasicProperties;
import com.yun.mq.mqserver.core.ExchangeType;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;

/**
 * @author yun
 * @date 2024/11/1 20:42
 * @desciption: 客户端测试
 */
@SpringBootTest
public class MqClientTest {

    private BrokerServer brokerServer = null;
    private ConnectionFactory connectionFactory = null;

    private Thread t = null;

    @BeforeEach
    public void setUp() throws IOException {
        MqApplication.context = SpringApplication.run(MqApplication.class);
        brokerServer = new BrokerServer(9090);
        t = new Thread(() -> {
            try {
                brokerServer.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        t.start();
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("127.0.0.1");
        connectionFactory.setPort(9090);
    }

    @AfterEach
    public void tearDown() throws IOException {
        brokerServer.stop();
        MqApplication.context.close();
        File file = new File("./data");
        FileUtils.deleteDirectory(file);
        connectionFactory = null;

    }

    @Test
    public void testConnection() throws IOException {
        Connection connection = connectionFactory.newConnection();
        Assertions.assertNotNull(connection);
        Channel channel = connection.createChannel();
        Assertions.assertNotNull(channel);

        connection.close();
        channel.close();
    }

    @Test
    public void testExchange() throws IOException {
        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();

        boolean b = channel.exchangeDeclare("testExchange", ExchangeType.DIRECT, true, true, null);
        Assertions.assertTrue(b);

        boolean b1 = channel.exchangeDelete("testExchange");
        Assertions.assertTrue(b1);

        channel.close();
        connection.close();
    }

    @Test
    public void testQueueAndBinding() throws IOException {

        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();

        boolean ok = channel.queueDeclare("testQueue", true, true, true, null);
        Assertions.assertTrue(ok);
        channel.exchangeDeclare("testExchange", ExchangeType.DIRECT, true, true, null);
        ok = channel.queueBind("testQueue", "testExchange", "testBinding");
        Assertions.assertTrue(ok);
        ok = channel.queueDelete("testQueue");
        Assertions.assertTrue(ok);
        ok = channel.queueUnbind("testExchange", "testQueue");
        Assertions.assertTrue(ok);
    }

    @Test
    public void testMessageFan() throws IOException, MqException, InterruptedException {
        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();

        boolean ok = channel.queueDeclare("testQueue", true, true, true, null);
        Assertions.assertTrue(ok);
        channel.exchangeDeclare("testExchange", ExchangeType.FANOUT, true, true, null);
        ok = channel.queueBind("testQueue", "testExchange", "testBinding");
        Assertions.assertTrue(ok);
        channel.queueDeclare("queue2", true, true, true, null);
        channel.queueBind("queue2", "testExchange", "binding2");

        ok=channel.basicPublish("testExchange", "", null, null);
        Assertions.assertTrue(ok);
        ok=channel.basicConsume("testQueue", true, (String consumerTag, BasicProperties basicProperties, byte[] body)->{

            System.out.println("gogoogogogogogoogogogo");
        });

        Channel channel1 = connection.createChannel();
        channel1.basicConsume("queue2", true, new Consumer() {
            @Override
            public void handleDelivery(String consumerTag, BasicProperties basicProperties, byte[] body) throws IOException, MqException {
                System.out.println("222222222222222222222222222222222222");
            }
        });


        Assertions.assertTrue(ok);
        Thread.sleep(100);
    }

    @Test
    public void testMessageDirect() throws IOException, MqException, InterruptedException {
        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();

        boolean ok = channel.queueDeclare("testQueue", true, true, true, null);
        Assertions.assertTrue(ok);
        channel.exchangeDeclare("testExchange", ExchangeType.DIRECT, true, true, null);

        ok=channel.basicPublish("testExchange", "testQueue", null, null);
        Assertions.assertTrue(ok);
        ok=channel.basicConsume("testQueue", true, (String consumerTag, BasicProperties basicProperties, byte[] body)->{

            System.out.println("gogoogogogogogoogogogo");
        });


        Assertions.assertTrue(ok);
        Thread.sleep(100);
    }

    @Test
    public void testMessageTopic() throws IOException, MqException, InterruptedException {
        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();

        boolean ok = channel.queueDeclare("testQueue", true, true, true, null);
        Assertions.assertTrue(ok);
        channel.exchangeDeclare("testExchange", ExchangeType.TOPIC, true, true, null);
        ok = channel.queueBind("testQueue", "testExchange", "yun");
        Assertions.assertTrue(ok);


        ok=channel.basicPublish("testExchange", "yun", null, null);
        Assertions.assertTrue(ok);
        ok=channel.basicConsume("testQueue", true, (String consumerTag, BasicProperties basicProperties, byte[] body)->{

            System.out.println("gogoogogogogogoogogogo");
        });


        Assertions.assertTrue(ok);
        Thread.sleep(100);
    }

}

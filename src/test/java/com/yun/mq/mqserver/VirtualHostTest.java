package com.yun.mq.mqserver;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.introspect.VirtualAnnotatedMember;
import com.yun.mq.MqApplication;
import com.yun.mq.common.Consumer;
import com.yun.mq.mqserver.core.BasicProperties;
import com.yun.mq.mqserver.core.Exchange;
import com.yun.mq.mqserver.core.ExchangeType;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.shadow.com.univocity.parsers.common.ContextSnapshot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yun
 * @date 2024/10/29 17:00
 * @desciption:
 */
@SpringBootTest
class VirtualHostTest {

    private VirtualHost virtualHost = null;

    @BeforeEach
    public void setUp() {
        MqApplication.context = SpringApplication.run(MqApplication.class);
        virtualHost = new VirtualHost("default");
    }

    @AfterEach
    public void tearDown() throws IOException {

        MqApplication.context.close();
        File data = new File("./data");
        FileUtils.deleteDirectory(data);
        virtualHost = null;

    }

    @Test
    public void testExchangeDeclareAndDelete() {
        boolean testExchange1 = virtualHost.exchangeDeclare("exchange1", ExchangeType.TOPIC, true, false, null);
        Assertions.assertTrue(testExchange1);
        boolean exchange1 = virtualHost.exchangeDelete("exchange1");
        Assertions.assertTrue(exchange1);
    }

    @Test
    public void testQueueDeclareAndDelete() {
        boolean testQueue = virtualHost.queueDeclare("testQueue", true, false, false, null);
        Assertions.assertTrue(testQueue);
        boolean testQueue1 = virtualHost.queueDelete("testQueue");
        Assertions.assertTrue(testQueue1);
    }

    @Test
    public void testBind() {
        virtualHost.exchangeDeclare("exchange1", ExchangeType.TOPIC, true, false, null);
        virtualHost.queueDeclare("testQueue", true, false, false, null);
        boolean b = virtualHost.queueBind("testQueue", "exchange1", "testBinding");
        Assertions.assertTrue(b);

        virtualHost.queueUnbind("exchange1", "testQueue");

    }

    // 测试发布消息
    @Test
    public void testBasicPublish() {
        virtualHost.exchangeDeclare("testExchange", ExchangeType.DIRECT, true, false, null);
        virtualHost.queueDeclare("testQueue", true, false, false, null);
        boolean b = virtualHost.basicPublish("testExchange", "testQueue", null, null);
        Assertions.assertTrue(b);

    }


    // 先订阅再发布消息
    @Test
    public void testBasicConsume1() throws InterruptedException {

        virtualHost.exchangeDeclare("testExchange", ExchangeType.DIRECT, true, false, null);
        virtualHost.queueDeclare("testQueue", true, false, false, null);

        boolean b1 = virtualHost.basicConsume("testConsumer", "testQueue", true, new Consumer() {
            @Override
            public void handleDelivery(String consumerTag, BasicProperties basicProperties, byte[] body) {
                System.out.println("messageId=" + basicProperties.getMessageId());
                System.out.println("body=" + new String(body, 0, body.length));

                assertEquals("testQueue", basicProperties.getRoutingKey());
                assertEquals(1, basicProperties.getDeliverMode());
            }
        });

        Assertions.assertTrue(b1);

        Thread.sleep(500);

        boolean b = virtualHost.basicPublish("testExchange", "testQueue", null, "hello_world".getBytes());
        Assertions.assertTrue(b);

    }

    // 先发布再订阅
    @Test
    public void testBasicConsume2() throws InterruptedException {

        virtualHost.exchangeDeclare("testExchange", ExchangeType.DIRECT, true, false, null);
        virtualHost.queueDeclare("testQueue", true, false, false, null);

        boolean b = virtualHost.basicPublish("testExchange", "testQueue", null, "hello_world".getBytes());
        Assertions.assertTrue(b);

        boolean b1 = virtualHost.basicConsume("testConsumer", "testQueue", true, new Consumer() {
            @Override
            public void handleDelivery(String consumerTag, BasicProperties basicProperties, byte[] body) {
                System.out.println("messageId=" + basicProperties.getMessageId());
                System.out.println("body=" + new String(body, 0, body.length));

                assertEquals("testQueue", basicProperties.getRoutingKey());
                assertEquals(1, basicProperties.getDeliverMode());
            }
        });

        Assertions.assertTrue(b1);


    }

    // 测试扇出交换机
    @Test
    public void testBasicConsumeFanOut() {
        // 先建立交换机
        virtualHost.exchangeDeclare("testExchange", ExchangeType.FANOUT, true, true, null);

        // 建立两个队列
        virtualHost.queueDeclare("testQueue1", true, false, true, null);
        virtualHost.queueDeclare("testQueue2", true, false, true, null);

        // 建立两个绑定
        // 因为是删除交换机 只要是绑定了的队列都要转发消息 所以bindingKey没什么用因此赋为空串即可
        virtualHost.queueBind("testQueue1", "testExchange", "");
        virtualHost.queueBind("testQueue2", "testExchange", "");

        // 发布消息
        virtualHost.basicPublish("testExchange", "testRoutingKey", null, "hello".getBytes());

        // 订阅队列
        boolean b = virtualHost.basicConsume("consumeTag1", "testQueue1", true, new Consumer() {
            @Override
            public void handleDelivery(String consumerTag, BasicProperties basicProperties, byte[] body) {
                System.out.println("consumeTag=" + consumerTag);
                System.out.println("messageId=" + basicProperties.getMessageId());
                assertEquals("hello", new String(body));
            }
        });

        Assertions.assertTrue(b);

        boolean b1 = virtualHost.basicConsume("consumeTag2", "testQueue2", true, new Consumer() {
            @Override
            public void handleDelivery(String consumerTag, BasicProperties basicProperties, byte[] body) {
                System.out.println("consumeTag=" + consumerTag);
                System.out.println("message=" + basicProperties.getMessageId());

                assertEquals("hello", new String(body));
            }
        });

        Assertions.assertTrue(b1);

    }

    // 测试主题交换机
    @Test
    public void testBasicConsumeTopic() {

        // 创建交换机
        virtualHost.exchangeDeclare("testExchange", ExchangeType.TOPIC, true, false, null);
        // 创建队列
        virtualHost.queueDeclare("testQueue", true, false, false, null);

        // 创建绑定
        virtualHost.queueBind("testQueue", "testExchange", "aaa.*.ddd");

        // 发布消息
        virtualHost.basicPublish("testExchange", "aaa.ds.ddd", null, "hello".getBytes());

        // 消费消息
        boolean b = virtualHost.basicConsume("consumeTag", "testQueue", true, new Consumer() {
            @Override
            public void handleDelivery(String consumerTag, BasicProperties basicProperties, byte[] body) {
                System.out.println("consumerTag=" + consumerTag);
                System.out.println("messageId=" + basicProperties.getMessageId());

                assertEquals(new String(body), "hello");
            }
        });

        Assertions.assertTrue(b);

    }

    @Test
    public void testBasicAck() throws InterruptedException {
        virtualHost.exchangeDeclare("testExchange", ExchangeType.DIRECT, true, false, null);
        virtualHost.queueDeclare("testQueue", true, false, false, null);

        boolean b = virtualHost.basicPublish("testExchange", "testQueue", null, "hello_world".getBytes());
        Assertions.assertTrue(b);

        boolean b1 = virtualHost.basicConsume("testConsumer", "testQueue", true, new Consumer() {
            @Override
            public void handleDelivery(String consumerTag, BasicProperties basicProperties, byte[] body) {
                System.out.println("messageId=" + basicProperties.getMessageId());
                System.out.println("body=" + new String(body, 0, body.length));

                assertEquals("testQueue", basicProperties.getRoutingKey());
                assertEquals(1, basicProperties.getDeliverMode());

                boolean b2 = virtualHost.basicAck("testQueue", basicProperties.getMessageId());
                Assertions.assertTrue(b2);
            }


        });

        Assertions.assertTrue(b1);
        Thread.sleep(500);
    }

}
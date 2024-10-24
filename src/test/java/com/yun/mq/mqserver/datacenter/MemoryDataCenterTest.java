package com.yun.mq.mqserver.datacenter;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.yun.mq.MqApplication;
import com.yun.mq.common.MqException;
import com.yun.mq.mqserver.core.*;
import net.bytebuddy.implementation.auxiliary.MethodCallProxy;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yun
 * @date 2024/10/24 15:48
 * @desciption:
 */
@SpringBootTest
class MemoryDataCenterTest {

    private MemoryDataCenter memoryDataCenter = null;

    @BeforeEach
    public void setUp() {
        memoryDataCenter = new MemoryDataCenter();
    }

    @AfterEach
    public void tearDown() {
        memoryDataCenter = null;
    }

    // 创建一个测试队列
    private MSGQueue createTestQueue(String queueName) {
        MSGQueue queue = new MSGQueue();
        queue.setDurable(true);
        queue.setName(queueName);
        queue.setExclusive(false);
        queue.setAutoDelete(false);
        return queue;
    }

    // 创建一个测试交换机
    public Exchange createTestExchange(String exchangeName) {
        Exchange exchange = new Exchange();
        exchange.setExchangeType(ExchangeType.DIRECT);
        exchange.setDurable(true);
        exchange.setAutoDelete(false);
        exchange.setName(exchangeName);

        return exchange;
    }

    // 测试交换机创建 获取以及删除
    @Test
    public void testExchange() {
        Exchange expectExchange = createTestExchange("testExchange");
        memoryDataCenter.insertExchange(expectExchange);

        // 取出交换机
        Exchange actualExchange = memoryDataCenter.getExchange("testExchange");
        Assertions.assertEquals(actualExchange, expectExchange);

        // 删除交换机
        memoryDataCenter.deleteExchange("testExchange");
        // 取出交换机 理想状态下是为空的
        actualExchange = memoryDataCenter.getExchange("testExchange");

        Assertions.assertNull(actualExchange);

    }

    // 测试队列
    @Test
    public void testQueue() {
        MSGQueue expectedQueue = createTestQueue("testQueue");
        memoryDataCenter.insertQueue(expectedQueue);
        MSGQueue actualQueue = memoryDataCenter.getQueue("testQueue");
        Assertions.assertEquals(actualQueue, expectedQueue);

        memoryDataCenter.deleteQueue("testQueue");
        actualQueue = memoryDataCenter.getQueue("testQueue");

        Assertions.assertNull(actualQueue);
    }

    // 测试绑定
    @Test
    public void testBinding() throws MqException {
        Binding expectedBinding = new Binding();
        expectedBinding.setBindingKey("testBindingKey");
        expectedBinding.setQueueName("testQueue");
        expectedBinding.setExchangeName("testExchange");

        memoryDataCenter.insertBinding(expectedBinding);
        Binding actualBinding = memoryDataCenter.getBinding("testExchange", "testQueue");
        Assertions.assertEquals(actualBinding, expectedBinding);

        ConcurrentHashMap<String, Binding> actualbindings = memoryDataCenter.getBindings("testExchange");
        Assertions.assertEquals(1, actualbindings.size());

        memoryDataCenter.deleteBinding(expectedBinding);
        actualBinding = memoryDataCenter.getBinding("testExchange", "testQueue");
        Assertions.assertNull(actualBinding);

    }

    // 生成消息
    public Message createTestMessage(String content) {
        Message message = Message.createMessageWithId(null, "testRoutingKey", content.getBytes());
        return message;
    }

    // 测试消息的插入 删除以及获取
    @Test
    public void testMessage() {
        // 创建消息
        Message expectedMessage = createTestMessage("testMessage");
        // 插入消息
        memoryDataCenter.addMessage(expectedMessage);
        // 获取消息
        Message actualMessage = memoryDataCenter.getMessage(expectedMessage.getMessageId());

        Assertions.assertEquals(actualMessage, expectedMessage);

        // 删除消息
        memoryDataCenter.deleteMessage(actualMessage.getMessageId());
        actualMessage = memoryDataCenter.getMessage(actualMessage.getMessageId());
        Assertions.assertNull(actualMessage);
    }

    @Test
    public void testSendMessage() {
        MSGQueue queue = createTestQueue("testQueue");
        List<Message> expectedList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Message message = createTestMessage("testMessage" + i);
            memoryDataCenter.sendMessage(queue, message);
            expectedList.add(message);
        }

        List<Message> actualList = new ArrayList<>();
        while (true) {
            Message message = memoryDataCenter.pollMessage("testQueue");
            if (message == null) {
                break;
            }
            actualList.add(message);
        }

        Assertions.assertEquals(expectedList.size(), actualList.size());
        Assertions.assertEquals(expectedList.get(3), actualList.get(3));
    }

    @Test
    public void testMessageWaitAck() {
        Message expectedMessage = createTestMessage("testMessage");

        memoryDataCenter.addMessageWaitAck("testQueue", expectedMessage);

        Message actualMessage = memoryDataCenter.getMessageWaitAck("testQueue", expectedMessage.getMessageId());

        Assertions.assertEquals(actualMessage, expectedMessage);

        memoryDataCenter.deleteMessageWaitAck("testQueue", actualMessage.getMessageId());

        actualMessage = memoryDataCenter.getMessageWaitAck("testQueue", actualMessage.getMessageId());

        Assertions.assertNull(actualMessage);

    }

    @Test
    public void testRecovery() throws IOException, MqException, ClassNotFoundException {
        // 后续的数据库操作需要使用到
        MqApplication.context = SpringApplication.run(MqApplication.class);

        // 在硬盘上构造好数据
        DiskDataCenter diskDataCenter = new DiskDataCenter();
        diskDataCenter.init();

        // 构造数据
        Exchange expectedExchange = createTestExchange("testExchange");
        diskDataCenter.insertExchange(expectedExchange);

        MSGQueue expectedQueue = createTestQueue("testQueue");
        diskDataCenter.insertQueue(expectedQueue);

        Binding expectedBinding = new Binding();
        expectedBinding.setExchangeName("testExchange");
        expectedBinding.setBindingKey("testBindingKey");
        expectedBinding.setQueueName("testQueue");
        diskDataCenter.insertBinding(expectedBinding);

        Message expectedMessage = createTestMessage("testMessage");
        diskDataCenter.sendMessage(expectedQueue, expectedMessage);

        // 进行恢复操作
        memoryDataCenter.recovery(diskDataCenter);

        // 取出内存中的数据
        Exchange actualExchange = memoryDataCenter.getExchange("testExchange");
        Assertions.assertEquals(actualExchange.getExchangeType(), expectedExchange.getExchangeType());
        Binding actualBinding = memoryDataCenter.getBinding("testExchange", "testQueue");
        Assertions.assertEquals(actualBinding.getExchangeName(), expectedBinding.getExchangeName());
        MSGQueue actualQueue = memoryDataCenter.getQueue(expectedQueue.getName());
        Assertions.assertEquals(actualQueue.getName(), expectedQueue.getName());
        Message actualMessage = memoryDataCenter.getMessage(expectedMessage.getMessageId());
        Assertions.assertEquals(actualMessage.getRoutingKey(), expectedMessage.getRoutingKey());

        MqApplication.context.close();

        File dataDir=new File("./data");
        FileUtils.deleteDirectory(dataDir);
    }

}
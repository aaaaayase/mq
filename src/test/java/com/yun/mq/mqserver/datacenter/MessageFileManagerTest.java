package com.yun.mq.mqserver.datacenter;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.yun.mq.common.BinaryTool;
import com.yun.mq.common.MqException;
import com.yun.mq.mqserver.core.MSGQueue;
import com.yun.mq.mqserver.core.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.integration.IntegrationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yun
 * @date 2024/10/23 16:34
 * @desciption:
 */
@SpringBootTest
class MessageFileManagerTest {

    private MessageFileManager messageFileManager = new MessageFileManager();

    private static final String queueName1 = "testQueue1";
    private static final String queueName2 = "testQueue2";

    @BeforeEach
    public void setUp() throws IOException {

        messageFileManager.createQueueFiles(queueName1);
        messageFileManager.createQueueFiles(queueName2);
    }

    @AfterEach
    public void tearDowm() throws IOException {
        messageFileManager.destroyQueueFiles(queueName1);
        messageFileManager.destroyQueueFiles(queueName2);
    }

    @Test
    public void testCreateFiles() {
        File file = new File("./data/" + queueName1 + "/queue_data.txt");
        File file2 = new File("./data/" + queueName2 + "/queue_stat.txt");
        Assertions.assertEquals(true, file.isFile());
        Assertions.assertEquals(true, file2.isFile());
    }

    @Test
    public void testReadWriteStat() {
        MessageFileManager.Stat stat = new MessageFileManager.Stat();
        stat.validCount = 100;
        stat.totalCount = 50;

        ReflectionTestUtils.invokeMethod(messageFileManager, "writeStat", queueName1, stat);

        MessageFileManager.Stat newStat = (MessageFileManager.Stat) ReflectionTestUtils.invokeMethod(messageFileManager, "readStat", queueName1);

        Assertions.assertEquals(100, newStat.validCount);
        Assertions.assertEquals(50, newStat.totalCount);

        System.out.println("测试 readStat 和 writeStat 完成！");

    }

    public MSGQueue createTestQueue(String queueName) {
        MSGQueue queue = new MSGQueue();
        queue.setExclusive(false);
        queue.setName(queueName);
        queue.setAutoDelete(false);
        queue.setDurable(true);
        return queue;
    }

    private Message createTestMessage(String content) {
        return Message.createMessageWithId(null, "testRoutingKey", content.getBytes());
    }

    @Test
    public void testSendMessage() throws IOException, MqException, ClassNotFoundException {
        MSGQueue queue = createTestQueue(queueName1);
        Message message = createTestMessage("testMessage");

        messageFileManager.sendMessage(queue, message);
        MessageFileManager.Stat stat = ReflectionTestUtils.invokeMethod(messageFileManager, "readStat", queueName1);
        Assertions.assertEquals(1, stat.totalCount);

        LinkedList<Message> messages = messageFileManager.LoadAllMessageFromQueue(queue.getName());

        Message curMesssage = messages.get(0);

        System.out.println(message.getMessageId());
        Assertions.assertEquals(message.getMessageId(), curMesssage.getMessageId());

    }

    @Test
    public void testLoadAllMessage() throws IOException, MqException, ClassNotFoundException {
        MSGQueue queue = createTestQueue(queueName1);
        List<Message> expectedMessages = new LinkedList<>();
        for (int i = 0; i < 100; i++) {
            Message message = createTestMessage("testMessage" + i);
            expectedMessages.add(message);
            messageFileManager.sendMessage(queue, message);
        }

        LinkedList<Message> actualMessages = messageFileManager.LoadAllMessageFromQueue(queueName1);
        Assertions.assertEquals(actualMessages.size(), expectedMessages.size());

        for (int i = 0; i < 100; i++) {
            Message expectedMessage = expectedMessages.get(i);
            Message actualMessage = actualMessages.get(i);

            System.out.println("[" + i + "]" + actualMessage);

            Assertions.assertEquals(actualMessage.getMessageId(), expectedMessage.getMessageId());

        }

    }

    @Test
    public void testDeleteMessage() throws IOException, MqException, ClassNotFoundException {
        MSGQueue queue = createTestQueue(queueName1);
        List<Message> expectMessages = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            Message message = createTestMessage("testMessage" + i);
            expectMessages.add(message);
            messageFileManager.sendMessage(queue, message);
        }

        messageFileManager.deleteMessage(queue, expectMessages.get(9));
        messageFileManager.deleteMessage(queue, expectMessages.get(8));
        messageFileManager.deleteMessage(queue, expectMessages.get(7));

        LinkedList<Message> messages = messageFileManager.LoadAllMessageFromQueue(queueName1);

        Assertions.assertEquals(7, messages.size());
    }

    @Test
    public void testGC() throws IOException, MqException, ClassNotFoundException {
        MSGQueue queue = createTestQueue(queueName1);
        List<Message> list = new LinkedList<>();
        for (int i = 0; i < 100; i++) {
            Message message = createTestMessage("testMessage" + i);
            list.add(message);
            messageFileManager.sendMessage(queue, message);
        }

        for (int i = 0; i < 100; i += 2) {
            messageFileManager.deleteMessage(queue, list.get(i));
        }

        messageFileManager.gc(queue);

        List<Message> list2 = messageFileManager.LoadAllMessageFromQueue(queue.getName());

        Assertions.assertEquals(50, list2.size());
        for (int i = 0; i < list2.size(); i++) {
            System.out.println(list2.get(i).getBody());
        }

    }

}
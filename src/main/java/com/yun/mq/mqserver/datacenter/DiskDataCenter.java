package com.yun.mq.mqserver.datacenter;

import com.yun.mq.common.MqException;
import com.yun.mq.mqserver.core.Binding;
import com.yun.mq.mqserver.core.Exchange;
import com.yun.mq.mqserver.core.MSGQueue;
import com.yun.mq.mqserver.core.Message;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author yun
 * @date 2024/10/24 8:40
 * @desciption: 用于管理硬盘上的所有数据
 * 1. 存储在数据库的队列 绑定 交换机
 * 2. 存储在文件上的消息
 */
public class DiskDataCenter {

    private DataBaseManager dataBaseManager = new DataBaseManager();
    private MessageFileManager messageFileManager = new MessageFileManager();

    public void init() {
        dataBaseManager.init();
        messageFileManager.init();
    }

    // 封装交换机操作
    public void insertExchange(Exchange exchange) {
        dataBaseManager.insertExchange(exchange);
    }

    public void deleteExchange(String exchangeName) {
        dataBaseManager.deleteExchange(exchangeName);
    }

    public List<Exchange> selectAllExchanges() {
        return dataBaseManager.selectAllExchanges();
    }

    // 封装队列操作
    public void insertQueue(MSGQueue queue) throws IOException {
        dataBaseManager.insertQueue(queue);
        // 创建队列的同时 不仅要把队列对象写到数据库 还需要创建相应的目录和文件
        messageFileManager.createQueueFiles(queue.getName());
    }

    public void deleteQueue(String queueName) throws IOException {
        dataBaseManager.deleteQueue(queueName);
        // 删除队列的同时 不仅需要将队列从数据库中删除 也需要去删除对应的目录以及文件
        messageFileManager.destroyQueueFiles(queueName);
    }

    public List<MSGQueue> selectAllQueues() {
        return dataBaseManager.selectAllQueues();
    }

    // 封装绑定操作
    public void insertBinding(Binding binding) {
        dataBaseManager.insertBinding(binding);
    }

    public void deleteBinding(Binding binding) {
        dataBaseManager.deleteBinding(binding);
    }

    public List<Binding> selectAllBindings() {
        return dataBaseManager.selectAllBindings();
    }

    // 封装消息操作
    public void sendMessage(MSGQueue queue, Message message) throws IOException, MqException {
        messageFileManager.sendMessage(queue, message);
    }

    public void deleteMessage(MSGQueue queue, Message message) throws IOException, ClassNotFoundException, MqException {
        messageFileManager.deleteMessage(queue, message);
        if (messageFileManager.checkGC(queue.getName())) {
            messageFileManager.gc(queue);
        }
    }

    public LinkedList<Message> loadAllMessagesFromQueue(String queueName) throws IOException, MqException, ClassNotFoundException {
        return messageFileManager.LoadAllMessageFromQueue(queueName);
    }

}

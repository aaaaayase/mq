package com.yun.mq.mqserver.datacenter;

import com.yun.mq.common.MqException;
import com.yun.mq.mqserver.core.Binding;
import com.yun.mq.mqserver.core.Exchange;
import com.yun.mq.mqserver.core.MSGQueue;
import com.yun.mq.mqserver.core.Message;

import java.io.IOException;
import java.io.StringReader;
import java.util.Currency;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yun
 * @date 2024/10/24 9:22
 * @desciption: 这个类来管理内存中的数据 此类的一些方法可能会在多线程环境下被使用，因此要注意线程安全问题
 */
public class MemoryDataCenter {

    // key是exchangeName value是Exchange类对象
    private ConcurrentHashMap<String, Exchange> exchangeMap = new ConcurrentHashMap<>();
    // key是queueName value是MSGqueue对象
    private ConcurrentHashMap<String, MSGQueue> queueMap = new ConcurrentHashMap<>();
    // 第一个key是exchangeName 第二个key是queueName
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Binding>> bindingsMap = new ConcurrentHashMap<>();
    // key是messageId value是message对象 消息中心
    private ConcurrentHashMap<String, Message> messageMap = new ConcurrentHashMap<>();
    // key是queueName value是一个Message的链表
    private ConcurrentHashMap<String, LinkedList<Message>> queueMessageMap = new ConcurrentHashMap<>();
    // 第一个key是queueName 第二个key是消息id 这个数据结构用来保存已读未回的那些消息
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Message>> queueMessageWaitAckMap = new ConcurrentHashMap<>();

    // 针对内存中的数据进行操作。
    // 插入交换机
    public void insertExchange(Exchange exchange) {
        exchangeMap.put(exchange.getName(), exchange);
        System.out.println("[MemoryDataCenter] 新交换机添加成功！ exchangeName=" + exchange.getName());
    }

    // 获取交换机
    public Exchange getExchange(String exchangeName) {
        return exchangeMap.get(exchangeName);
    }

    // 删除交换机
    public void deleteExchange(String exchangeName) {
        exchangeMap.remove(exchangeName);
        System.out.println("[MemoryDataCenter] 交换机删除成功！ exchangeName=" + exchangeName);
    }

    // 插入队列
    public void insertQueue(MSGQueue queue) {
        queueMap.put(queue.getName(), queue);
        System.out.println("[MemoryDataCenter] 新队列添加成功！ queueName=" + queue.getName());
    }

    // 获取队列
    public MSGQueue getQueue(String queueName) {
        return queueMap.get(queueName);
    }

    // 删除队列
    public void deleteQueue(String queueName) {
        queueMap.remove(queueName);
        System.out.println("[MemoryDataCenter] 队列删除成功！ queueName=" + queueName);
    }

    // 插入绑定
    public void insertBinding(Binding binding) throws MqException {
        // 首先检查哈希表是否存在
//        ConcurrentHashMap<String, Binding> bindingMap = bindingsMap.get(binding.getExchangeName());
//        if (bindingMap == null) {
//            bindingMap = new ConcurrentHashMap<>();
//            bindingsMap.put(binding.getExchangeName(), bindingMap);
//        }
        // 先用exchangeName查一下 对应的哈希表是否存在 不存在则创建一个 并且将创建的哈希的引用还是交给了bindingMap
        ConcurrentHashMap<String, Binding> bindingMap = bindingsMap.computeIfAbsent(binding.getExchangeName(), k -> new ConcurrentHashMap<>());

        synchronized (bindingMap) {
            // 检查对应的绑定关系是否存在 存在则抛异常
            if (bindingMap.get(binding.getQueueName()) != null) {
                throw new MqException("[MemoryDataCenter] 绑定已经存在！ exchangeName=" + binding.getExchangeName() + " queueName=" + binding.getQueueName());
            }
            bindingMap.put(binding.getQueueName(), binding);
        }
        System.out.println("[MemoryDataCenter] 绑定添加成功！ exchangeName=" + binding.getExchangeName() + " queueName=" + binding.getQueueName());
    }

    // 获取绑定 分别写两种方式的获取绑定的方法
    // 1. 根据queueName以及exchangeName来获取
    // 2. 根据queueName来获取全部的绑定
    public Binding getBinding(String exchangeName, String queueName) {
        ConcurrentHashMap<String, Binding> bindingMap = bindingsMap.get(exchangeName);
        if (bindingMap == null) {
            return null;
        }
        Binding binding = bindingMap.get(queueName);
        return binding;
    }

    public ConcurrentHashMap<String, Binding> getBindings(String exchangeName) {
        return bindingsMap.get(exchangeName);
    }

    // 删除绑定
    public void deleteBinding(Binding binding) throws MqException {
        ConcurrentHashMap<String, Binding> bindingMap = bindingsMap.get(binding.getExchangeName());
        if (bindingMap == null) {
            throw new MqException("[MemoryDataCenter] 绑定不存在！ exchangeName=" + binding.getExchangeName() + " queueName=" + binding.getQueueName());
        }
        bindingMap.remove(binding.getQueueName());
        System.out.println("[MemoryDataCenter] 绑定删除成功！ exchangeName=" + binding.getExchangeName() + " queueName=" + binding.getQueueName());
    }

    // 添加消息
    public void addMessage(Message message) {
        messageMap.put(message.getMessageId(), message);
        System.out.println("[MemoryDataCenter] 消息添加成功！ messageId=" + message.getMessageId());
    }

    // 获取消息
    public Message getMessage(String messageId) {
        return messageMap.get(messageId);
    }

    // 根据id删除消息
    public void deleteMessage(String messageId) {
        messageMap.remove(messageId);
        System.out.println("[MemoryDataCenter] 消息删除成功！ messageId=" + messageId);
    }

    // 发送消息到指定队列
    public void sendMessage(MSGQueue queue, Message message) {
        // 根据队列名找到指定的链表
        // 这一步需要判断链表是否为空 直接使用computeIfAbsent方法以及lambda表达式
        LinkedList<Message> messages = queueMessageMap.computeIfAbsent(queue.getName(), k -> new LinkedList<>());
        // 把消息加入链表 这里需要加锁 因为链表是线程不安全的
        synchronized (messages) {
            messages.add(message);
        }
        // 同时也将消息加入消息中心 这里即使消息重复也没有关系 因为哈希内部就是这样的 id相同就会把之前的value给覆盖掉
        addMessage(message);
        // 打印日志
        System.out.println("[MemoryDataCenter] 消息已发送到相应队列！ messageId=" + message.getMessageId());
    }

    // 从指定队列中取出消息
    public Message pollMessage(String queueName) {
        // 从指定队列中取出链表
        LinkedList<Message> messages = queueMessageMap.get(queueName);
        if (messages == null) {
            return null;
        }

        // 接下来是对链表的操作 需要加锁
        synchronized (messages) {
            if (messages.size() == 0) {
                return null;
            }

            Message currentMessage = messages.remove(0);
            System.out.println("[MemoryDataCenter] 消息已从队列中取出！ messageId=" + currentMessage.getMessageId());
            return currentMessage;
        }

    }

    // 返回相应队列中的消息个数
    public int getMessageCount(String queueName) {
        LinkedList<Message> messages = queueMessageMap.get(queueName);
        if (messages == null) {
            return 0;
        }
        // 这里还是对链表的操作 需要加锁
        synchronized (messages) {
            return messages.size();
        }
    }

    // 添加未确认的消息
    public void addMessageWaitAck(String queueName, Message message) {
        ConcurrentHashMap<String, Message> messageHashMap = queueMessageWaitAckMap.computeIfAbsent(queueName, k -> new ConcurrentHashMap<>());
        messageHashMap.put(message.getMessageId(), message);
        System.out.println("[MemoryDataCenter] 消息已进入待确认队列！ messageId=" + message.getMessageId());
    }

    // 删除未确认的消息
    public void deleteMessageWaitAck(String queueName, String messageId) {
        ConcurrentHashMap<String, Message> messageHashMap = queueMessageWaitAckMap.get(queueName);
        if (messageHashMap == null) {
            return;
        }
        messageHashMap.remove(messageId);
        System.out.println("[MemoryDataCenter] 消息从待确认队列删除！ messageId=" + messageId);
    }

    // 获取指定的未确认的消息
    public Message getMessageWaitAck(String queueName, String messageId) {
        ConcurrentHashMap<String, Message> messageHashMap = queueMessageWaitAckMap.get(queueName);
        if (messageHashMap == null) {
            return null;
        }

        return messageHashMap.get(messageId);
    }

    // 从硬盘中读取数据到内存中 可以理解成就是将硬盘中的数据赋给内存中的这些数据结构
    public void recovery(DiskDataCenter diskDataCenter) throws IOException, MqException, ClassNotFoundException {
        // 0. 清空之前的所有数据
        exchangeMap.clear();
        queueMap.clear();
        bindingsMap.clear();
        messageMap.clear();
        queueMessageMap.clear();
        // 1. 恢复交换机数据
        List<Exchange> exchanges = diskDataCenter.selectAllExchanges();
        for (Exchange exchange : exchanges) {
            exchangeMap.put(exchange.getName(), exchange);
        }

        // 2. 恢复队列数据
        List<MSGQueue> queues = diskDataCenter.selectAllQueues();
        for (MSGQueue queue : queues) {
            queueMap.put(queue.getName(), queue);
        }

        // 3. 恢复绑定数据
        List<Binding> bindings = diskDataCenter.selectAllBindings();
        for (Binding binding : bindings) {
            ConcurrentHashMap<String, Binding> bindingMap = bindingsMap.computeIfAbsent(binding.getExchangeName(), k -> new ConcurrentHashMap<>());
            bindingMap.put(binding.getQueueName(), binding);
        }

        // 4. 恢复消息数据
        // 遍历队列 通过队列名来获取相应的消息
        for (MSGQueue queue : queues) {
            LinkedList<Message> messages = diskDataCenter.loadAllMessagesFromQueue(queue.getName());
            for (Message message : messages) {
                messageMap.put(message.getMessageId(), message);
            }
            queueMessageMap.put(queue.getName(), messages);
        }

        // 针对没收到确认的那部分消息 当出现服务器重启的情况时不需要从硬盘恢复
        // 当服务器重启 这些没收到的确认的消息转为未被取走的消息
        // 消费者可以重复之前的操作 再去取走同样的消息 因为此时客户端（消费者）处理到一半终止了 与其再将其恢复到初始的状态不如让它重新做一遍
    }

}

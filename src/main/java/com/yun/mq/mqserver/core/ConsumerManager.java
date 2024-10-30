package com.yun.mq.mqserver.core;

import com.yun.mq.common.Consumer;
import com.yun.mq.common.ConsumerEnv;
import com.yun.mq.common.MqException;
import com.yun.mq.mqserver.VirtualHost;
import org.apache.catalina.startup.CopyParentClassLoaderRule;
import org.apache.ibatis.annotations.Param;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author yun
 * @date 2024/10/29 13:08
 * @desciption: 通过这个类来实现消费消息的核心逻辑
 */
public class ConsumerManager {
    // 持有上层的VirtualHost对象的引用 用来操作数据
    private VirtualHost parent;

    // 指定一个线程池来执行具体的回调任务
    ExecutorService workerPool = Executors.newFixedThreadPool(4);

    // 存放已经有消息的队列名的队列‘
    BlockingDeque<String> tokenQueue = new LinkedBlockingDeque<>();

    // 一个线程用来扫描存放当前有消息队列的队列
    private Thread scannerThread = null;

    public ConsumerManager(VirtualHost p) {
        this.parent = p;

        // 初始化扫描线程 指定它要做的事情
        scannerThread = new Thread(() -> {
            while (true) {
                try {
                    // 1. 从存放队列名的队列中拿到有消息的队列名
                    String queueName = tokenQueue.take();
                    // 2. 根据队列名来查找内存中的队列
                    MSGQueue queue = parent.getMemoryDataCenter().getQueue(queueName);

                    // 3. 判断队列是否存在
                    if (queue == null) {
                        throw new MqException("[ConsumerManager] 取出队列名后发现，发现该队列不存在！ queueName=" + queueName);
                    }

                    // 4. 队列存在 那么就让消费者消费队列中的消息
                    synchronized (queue) {
                        consumeMessage(queue);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        scannerThread.setDaemon(true);
        scannerThread.start();
    }

    // 这个方法调用时机就是发送消息的时候 通知消费
    public void notifyConsume(String queueName) throws InterruptedException {
        tokenQueue.put(queueName);
    }

    // 给队列添加订阅者
    public void addConsumer(String consumeTag, String queueName, boolean autoAck, Consumer consumer) throws MqException {
        // 创建ConsumerEnv对象
        ConsumerEnv consumerEnv = new ConsumerEnv(consumeTag, queueName, autoAck, consumer);
        // 获取队列
        MSGQueue queue = parent.getMemoryDataCenter().getQueue(queueName);
        // 队列不存在
        if (queue == null) {
            throw new MqException("[ConsumerManager] 队列不存在！ queueName=" + queueName);
        }

        synchronized (queue) {
            // 将创建的对象加入队列
            queue.addConsumerEnv(consumerEnv);
            // 如果队列已经有消息了就需要立刻去消费掉
            int n = parent.getMemoryDataCenter().getMessageCount(queueName);
            for (int i = 0; i < n; i++) {
                // 消费消息
                consumeMessage(queue);
            }
        }


    }


    // 消费消息
    private void consumeMessage(MSGQueue queue) {
        // 1. 按照轮询的方式来取出队列的消费者即消费者 轮询是在chooseEnv函数内部实现的
        ConsumerEnv luckyDog = queue.chooseConsumer();
        // 2. 验证取到的队列消费者
        if (luckyDog == null) {
            // 说明此时没有订阅该队列的消费者
            return;
        }
        // 3. 获取队列上的消息
        Message message = parent.getMemoryDataCenter().pollMessage(queue.getName());
        // 4. 判断消息是否存在
        if (message == null) {
            // 队列上无消息
            return;
        }

        // 5. 队列上有消息 使用线程池中的线程来执行消息
        workerPool.submit(() -> {
            try {
                // 5.1 调用回调函数consumer之前 先将消息存放到待回复集合中 这样做是为了防止回调函数出问题 丢失消息
                parent.getMemoryDataCenter().addMessageWaitAck(queue.getName(), message);
                // 5.2 调用回调函数
                luckyDog.getConsumer().handleDelivery(luckyDog.getConsumerTag(), message.getBasicProperties(), message.getBody());
                // 5.3 判断消费者消费消息之后是自动回复还是手动回复
                if (luckyDog.isAutoAck()) {
                    // 是自动回复 那么在执行完回调函数之后需要删除消息
                    // 删除硬盘上的消息
                    if (message.getDeliverMode() == 2) {
                        parent.getDiskDataCenter().deleteMessage(queue, message);
                    }
                    // 删除待确认集合中的消息
                    parent.getMemoryDataCenter().deleteMessageWaitAck(queue.getName(), message.getMessageId());
                    // 删除消息中心中的消息
                    parent.getMemoryDataCenter().deleteMessage(message.getMessageId());
                    System.out.println("[ConsumerManager] 消息被成功消费！queueName=" + queue.getName());
                }
                // 手动回复
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }
}

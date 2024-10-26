package com.yun.mq.mqserver;

import com.yun.mq.common.MqException;
import com.yun.mq.mqserver.core.*;
import com.yun.mq.mqserver.datacenter.DiskDataCenter;
import com.yun.mq.mqserver.datacenter.MemoryDataCenter;
import org.apache.tomcat.util.security.Escape;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.util.RouteMatcher;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yun
 * @date 2024/10/25 9:37
 * @desciption: 虚拟主机类于数据库中的database的功能
 * 管理自己的交换机 绑定 消息以及队列
 */
public class VitualHost {

    // 主机名
    private String virtualHostName;

    // 管理硬盘数据
    private DiskDataCenter diskDataCenter = new DiskDataCenter();

    // 管理内存数据
    private MemoryDataCenter memoryDataCenter = new MemoryDataCenter();

    private Router router = new Router();

    private Object exchangeLocker = new Object();

    private Object queueLocker = new Object();

    public String getVitualName() {
        return virtualHostName;
    }

    public DiskDataCenter getDiskDataCenter() {
        return diskDataCenter;
    }

    public MemoryDataCenter getMemoryDataCenter() {
        return memoryDataCenter;
    }

    public VitualHost(String name) {
        this.virtualHostName = name;

        // 对于MemoryFileManager来说不需要初始化 对象new出来即可
        // 但是DiskDataCenter则是需要去初始化 建库建表等等
        diskDataCenter.init();

        // 另外还需要将硬盘上的数据恢复到内存
        try {
            memoryDataCenter.recovery(diskDataCenter);
        } catch (IOException | MqException | ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("[VirtualHost] 恢复内存数据失败！");
        }
    }

    // 创建交换机 成功则返回true 失败则返回false
    public boolean exchangeDeclare(String exchangeName, ExchangeType exchangeType, boolean durable, boolean autoDelete, Map<String, Object> arguments) {
        // 给交换机名加上虚拟主机名的前缀
        exchangeName = virtualHostName + exchangeName;

        try {
            synchronized (exchangeLocker) {
                // 1. 判定此时内存中是否存在同名的交换机
                Exchange existsExchange = memoryDataCenter.getExchange(exchangeName);
                if (existsExchange != null) {
                    System.out.println("[VirtualHost] 交换机已经存在！ exchangeName=" + exchangeName);
                    return true;
                }

                // 2.创建交换机对象并且进行初始化
                Exchange exchange = new Exchange();
                exchange.setName(exchangeName);
                exchange.setDurable(durable);
                exchange.setAutoDelete(autoDelete);
                exchange.setArguments(arguments);
                exchange.setExchangeType(exchangeType);

                // 3. 将交换机写入内存以及硬盘 先写入硬盘 因为硬盘容易出错 如果先写入内存成功之后写入硬盘又出错还得恢复内存
                if (durable) {
                    diskDataCenter.insertExchange(exchange);
                }

                // 4. 把交换机对象写入内存
                memoryDataCenter.insertExchange(exchange);
                System.out.println("[VirtualHost] 交换机创建完成！ exchangeName=" + exchangeName);
            }
            return true;
        } catch (Exception e) {

            System.out.println("[VirtualHost] 交换机创建失败！ exchangeName=" + exchangeName);
            e.printStackTrace();
            return false;
        }
    }

    // 删除交换机
    public boolean exchangeDelete(String exchangeName) {
        // 给交换机加上虚拟主机前缀
        exchangeName = virtualHostName + exchangeName;
        try {
            synchronized (exchangeLocker) {
                // 1. 查找内存中的交换机
                Exchange toDelete = memoryDataCenter.getExchange(exchangeName);
                if (toDelete == null) {
                    throw new MqException("[VirtualHost] 交换机不存在无法删除！");
                }
                // 2. 删除硬盘上的数据
                if (toDelete.isDurable()) {
                    diskDataCenter.deleteExchange(exchangeName);
                }

                // 3. 删除内存中的数据
                memoryDataCenter.deleteExchange(exchangeName);
                System.out.println("[VirtualHost] 交换机删除完成！ exchangeName=" + exchangeName);
            }
            return true;

        } catch (Exception e) {
            System.out.println("[VirtualHost] 交换机删除失败！ exchangeName=" + exchangeName);
            e.printStackTrace();
            return false;
        }
    }

    // 创建队列
    public boolean queueDeclare(String queueName, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments) {
        // 设置队列名 需要虚拟主机前缀
        queueName = virtualHostName + queueName;
        try {
            synchronized (queueLocker) {
                // 1.查找内存中的同名队列
                MSGQueue existsQueue = memoryDataCenter.getQueue(queueName);
                if (existsQueue != null) {
                    System.out.println("[VirtualHost] 队列已经存在！ queueName=" + queueName);
                    return true;
                }

                // 2. 建立真正的对象
                MSGQueue queue = new MSGQueue();
                queue.setExclusive(exclusive);
                queue.setDurable(durable);
                queue.setName(queueName);
                queue.setAutoDelete(autoDelete);
                queue.setArguments(arguments);

                // 3. 队列插入硬盘
                if (durable) {
                    diskDataCenter.insertQueue(queue);
                }
                // 4. 队列插入内存
                memoryDataCenter.insertQueue(queue);
                System.out.println("[VirtualHost] 队列创建完成！ queueName=" + queueName);
            }
            return true;
        } catch (Exception e) {
            System.out.println("[VirtualHost] 队列创建失败！ queueName=" + queueName);
            e.printStackTrace();
            return false;
        }
    }

    // 删除队列
    public boolean queueDelete(String queueName) {
        // 增加前缀
        queueName = virtualHostName + queueName;
        try {
            synchronized (queueLocker) {
                // 1. 查找内存中队列是否存在
                MSGQueue toDelete = memoryDataCenter.getQueue(queueName);
                if (toDelete == null) {
                    throw new MqException("[VirtualHost] 队列不存在无法删除！ queueName=" + queueName);
                }

                // 2. 删除磁盘中的队列
                if (toDelete.isDurable()) {
                    diskDataCenter.deleteQueue(queueName);
                }
                // 3. 删除内存中的队列
                memoryDataCenter.deleteQueue(queueName);
                System.out.println("[VirtualHost] 队列删除完成！ queueName=" + queueName);
            }
            return true;
        } catch (Exception e) {
            System.out.println("[VirtualHost] 队列删除失败！ queueName=" + queueName);
            e.printStackTrace();
            return false;
        }
    }

    // 创建绑定
    public boolean queueBind(String queueName, String exchangeName, String bindingKey) {
        queueName = virtualHostName + queueName;
        exchangeName = virtualHostName + exchangeName;

        try {
            synchronized (exchangeLocker) {
                synchronized (queueLocker) {
                    // 1. 查找内存中是否存在对应绑定
                    Binding existsBinding = memoryDataCenter.getBinding(exchangeName, queueName);
                    if (existsBinding != null) {
                        throw new MqException("[VirtualHost] binding已经存在！ exchangeName=" + exchangeName + " queueName=" + queueName);
                    }
                    // 2. 验证bindingKey是否合法
                    if (!router.checkBindingKey(bindingKey)) {
                        throw new MqException("[VirtualHost] bindingKey不合法！ bindingKey=" + bindingKey);
                    }
                    // 3. 建立binding对象
                    Binding binding = new Binding();
                    binding.setQueueName(queueName);
                    binding.setBindingKey(bindingKey);
                    binding.setExchangeName(exchangeName);

                    // 4. 获取一下绑定的交换机和队列 一个不存在则绑定就不合法
                    Exchange exchange = memoryDataCenter.getExchange(exchangeName);
                    if (exchange == null) {
                        throw new MqException("[VirtualHost] 交换机不存在！ exchangName=" + exchangeName);
                    }
                    MSGQueue queue = memoryDataCenter.getQueue(queueName);
                    if (queue == null) {
                        throw new MqException("[VirtualHost] 队列不存在！ queueName=" + queueName);
                    }

                    // 5. 将绑定写入硬盘 这里必须关联的交换机以及队列都是在硬盘中持久化的 否则这个绑定就是无意义的或者说不能存在硬盘中
                    if (queue.isDurable() && exchange.isDurable()) {
                        diskDataCenter.insertBinding(binding);
                    }
                    // 6. 将绑定写入内存
                    memoryDataCenter.insertBinding(binding);

                    System.out.println("[VirtualHost] 绑定创建完成！ exchangeName=" + exchangeName + " queueName=" + queueName);
                }
            }
            return true;

        } catch (Exception e) {
            System.out.println("[VirtualHost] 绑定创建失败！ exchangeName=" + exchangeName + " queueName=" + queueName);
            e.printStackTrace();
            return false;
        }

    }

    // 删除绑定
    public boolean queueUnbind(String exchangeName, String queueName) {
        // 加前缀
        exchangeName = virtualHostName + exchangeName;
        queueName = virtualHostName + queueName;

        try {
            synchronized (exchangeLocker) {
                synchronized (queueLocker) {
                    // 1. 查找是否存在
                    Binding binding = memoryDataCenter.getBinding(exchangeName, queueName);
                    if (binding == null) {
                        throw new MqException("[VirtualHost] 删除绑定失败！绑定不存在！ exchangeName=" + exchangeName + " queueName=" + queueName);
                    }
                    // 2. 删除硬盘中的绑定
                    diskDataCenter.deleteBinding(binding);

                    // 3. 删除内存中的绑定
                    memoryDataCenter.deleteBinding(binding);

                    System.out.println("[VirtualHost] 绑定删除成功！exchangeName=" + exchangeName + " queueName=" + queueName);
                }
            }

            return true;
        } catch (Exception e) {
            System.out.println("[VirtualHost] 绑定删除失败！ exchangeName=" + exchangeName + " queueName=" + queueName);
            e.printStackTrace();
            return false;
        }

    }

    // 从指定交换机发送消息到队列
    public boolean basicPublish(String exchangeName, String routingKey, BasicProperties basicProperties, byte[] body) {
        exchangeName = virtualHostName + exchangeName;
        try {
            // 1. 检查routingKey是否合法
            if (!router.checkRoutingKey(routingKey)) {
                throw new MqException("[VirtualHost] routingKey不合法！ routingKey=" + routingKey);
            }

            // 2. 查找交换机对象
            Exchange exchange = memoryDataCenter.getExchange(exchangeName);
            if (exchange == null) {
                throw new MqException("[VirtualHost] 交换机不存在！ exchangeName=" + exchangeName);
            }

            // 3. 判断交换机类型
            if (exchange.getExchangeType() == ExchangeType.DIRECT) {
                // 直接交换机 直接把routingKey当成队列名
                String queueName = virtualHostName + routingKey;
                // 获取队列对象
                MSGQueue queue = memoryDataCenter.getQueue(queueName);
                if (queue == null) {
                    throw new MqException("[VirtualHost] 队列不存在！ queueName=" + queueName);
                }

                // 创建消息
                Message message = Message.createMessageWithId(basicProperties, routingKey, body);

                // 队列存在则直接将消息发送至队列
                sendMessage(queue, message);

            } else {
                // 主题交换机以及扇出交换机
                // 获取绑定对象
                ConcurrentHashMap<String, Binding> bindingsMap = memoryDataCenter.getBindings(exchangeName);
                for (Map.Entry<String, Binding> entry : bindingsMap.entrySet()) {
                    Binding binding = entry.getValue();
                    MSGQueue queue = memoryDataCenter.getQueue(binding.getQueueName());
                    if (queue == null) {
                        // 这里不抛异常了 因为不希望因为一个队列的失败而去影响到其它消息
                        System.out.println("[VirtualHost] basicPublish发送消息时，发现队列不存在！ queueName=" + binding.getQueueName());
                        continue;
                    }

                    // 创建消息
                    Message message = Message.createMessageWithId(basicProperties, routingKey, body);
                    // 检验消息是否能够发送到队列
                    // fanout 消息要转发给所有绑定的队列
                    // topic 消息发送只需要考虑bindingKey以及routingKey
                    if (!router.route(exchange.getExchangeType(), binding, message)) {
                        continue;
                    }

                    // 真正发消息给队列
                    sendMessage(queue, message);

                }

            }

            System.out.println("[VirtualHost] 消息发送完成！ exchangeName=" + exchangeName);
            return true;
        } catch (Exception e) {
            System.out.println("[VirtualHost] 消息发送失败！ exchangeName=" + exchangeName);
            e.printStackTrace();
            return false;
        }
    }

    private void sendMessage(MSGQueue queue, Message message) throws IOException, MqException {
        int deliverMode = message.getDeliverMode();
        // 判断是否要将消息写到硬盘上
        if (deliverMode == 2) {
            // 写入硬盘
            diskDataCenter.sendMessage(queue, message);
        }
        // 写入内存
        memoryDataCenter.sendMessage(queue, message);

        // TODO 此处需要补充一个逻辑 提醒消费者可以消费消息了

    }

}

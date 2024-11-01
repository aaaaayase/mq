package com.yun.mq.mqclient;

import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.yun.mq.common.*;
import com.yun.mq.mqserver.core.BasicProperties;
import com.yun.mq.mqserver.core.ExchangeType;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yun
 * @date 2024/11/1 9:34
 * @desciption: 表示逻辑上的通信通道
 */
public class Channel {
    private String channelId;

    // 当前这个通道属于哪个连接
    private Connection connection;

    // 用一个集合来管理从服务器端返回的响应
    private ConcurrentHashMap<String, BasicReturns> basicReturnsMap = new ConcurrentHashMap<>();

    // 当订阅队列 队列推送消息 需要使用一个回调函数来处理消息
    // 这里约定 一个channel只能有一个回调
    private Consumer consumer = null;

    public Channel(String channelId, Connection connection) {
        this.channelId = channelId;
        this.connection = connection;
    }

    // 负责将客户端创建通道的消息告诉服务器那边 与服务器端进行交互
    public boolean createChannel() throws IOException {
        // 对于创建channel这样的操作 它的payload就是BasicArguments这个类型
        BasicArguments basicArguments = new BasicAckArguments();
        basicArguments.setChannelId(channelId);
        basicArguments.setRid(generateRid());

        Request request = new Request();
        request.setType(0x1);
        byte[] payload = BinaryTool.toBytes(basicArguments);
        request.setLength(payload.length);
        request.setPayload(payload);

        // 发送请求
        connection.writeRequest(request);

        // 等待服务器响应
        BasicReturns basicReturns = waitResult(basicArguments.getRid());

        return basicReturns.isOk();
    }

    // 使用这个方法来阻塞等待服务器的响应
    private BasicReturns waitResult(String rid) {
        BasicReturns basicReturns = null;
        while ((basicReturns = basicReturnsMap.get(rid)) == null) {
            // 如果查询结果为 null, 说明包裹还没回来.
            // 此时就需要阻塞等待.
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        // 读取成功之后 还需要从集合中删除相应的响应
        basicReturnsMap.remove(rid);
        return basicReturns;
    }


    public void putReturns(BasicReturns basicReturns) {
        basicReturnsMap.put(basicReturns.getRid(), basicReturns);
        synchronized (this) {
            // 当前不知道有多少线程在等待响应 因此直接唤醒全部的等待的线程
            notifyAll();
        }
    }

    private String generateRid() {
        return "R" + UUID.randomUUID().toString();
    }

    // 关闭channel
    public boolean close() throws IOException {
        // payload还是基础的参数
        BasicArguments basicArguments = new BasicAckArguments();
        basicArguments.setRid(generateRid());
        basicArguments.setChannelId(channelId);

        // 构造请求对象
        byte[] payload = BinaryTool.toBytes(basicArguments);
        Request request = new Request();
        request.setPayload(payload);
        request.setType(0x2);
        request.setLength(payload.length);
        connection.writeRequest(request);

        // 等待响应
        BasicReturns basicReturns = waitResult(basicArguments.getRid());
        return basicReturns.isOk();
    }

    // 创建交换机
    public boolean exchangeDeclare(String exchangeName, ExchangeType exchangeType, boolean durable, boolean autoDelete, Map<String, Object> arguments) throws IOException {
        // 构建参数 也就是payload
        ExchangeDeclareArguments exchangeDeclareArguments = new ExchangeDeclareArguments();
        exchangeDeclareArguments.setArguments(arguments);
        exchangeDeclareArguments.setExchangeType(exchangeType);
        exchangeDeclareArguments.setExchangeName(exchangeName);
        exchangeDeclareArguments.setDurable(durable);
        exchangeDeclareArguments.setAutoDelete(autoDelete);
        exchangeDeclareArguments.setChannelId(channelId);
        exchangeDeclareArguments.setRid(generateRid());

        // 序列化
        byte[] payload = BinaryTool.toBytes(exchangeDeclareArguments);
        // 构建请求对象
        Request request = new Request();
        request.setType(0x3);
        request.setLength(payload.length);
        request.setPayload(payload);

        // 发送请求
        connection.writeRequest(request);

        // 等待响应
        BasicReturns basicReturns = waitResult(exchangeDeclareArguments.getRid());

        return basicReturns.isOk();
    }

    // 删除交换机
    public boolean exchangeDelete(String exchangeName) throws IOException {
        // 构造参数对象
        ExchangeDeleteArguments exchangeDeleteArguments = new ExchangeDeleteArguments();
        exchangeDeleteArguments.setExchangeName(exchangeName);
        exchangeDeleteArguments.setChannelId(channelId);
        exchangeDeleteArguments.setRid(generateRid());

        byte[] payload = BinaryTool.toBytes(exchangeDeleteArguments);

        // 构造请求对象
        Request request = new Request();
        request.setPayload(payload);
        request.setLength(payload.length);
        request.setType(0x4);

        // 发送请求
        connection.writeRequest(request);

        // 等待响应
        BasicReturns basicReturns = waitResult(exchangeDeleteArguments.getRid());

        return basicReturns.isOk();
    }

    // 创建队列
    public boolean queueDeclare(String queueName, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments) throws IOException {
        // 构造参数对象 也就是payload
        QueueDeclareArguments queueDeclareArguments = new QueueDeclareArguments();
        queueDeclareArguments.setArguments(arguments);
        queueDeclareArguments.setQueueName(queueName);
        queueDeclareArguments.setDurable(durable);
        queueDeclareArguments.setExclusive(exclusive);
        queueDeclareArguments.setAutoDelete(autoDelete);
        queueDeclareArguments.setChannelId(channelId);
        queueDeclareArguments.setRid(generateRid());

        byte[] payload = BinaryTool.toBytes(queueDeclareArguments);

        // 构造请求
        Request request = new Request();
        request.setType(0x5);
        request.setLength(payload.length);
        request.setPayload(payload);

        // 发送请求
        connection.writeRequest(request);

        // 等待响应
        BasicReturns basicReturns = waitResult(queueDeclareArguments.getRid());

        return basicReturns.isOk();
    }

    // 删除队列
    public boolean queueDelete(String queueName) throws IOException {
        // 构造参数对象
        QueueDeleteArguments queueDeleteArguments = new QueueDeleteArguments();
        queueDeleteArguments.setQueueName(queueName);
        queueDeleteArguments.setChannelId(channelId);
        queueDeleteArguments.setRid(generateRid());

        // 序列化
        byte[] payload = BinaryTool.toBytes(queueDeleteArguments);

        // 构造请求
        Request request = new Request();
        request.setPayload(payload);
        request.setLength(payload.length);
        request.setType(0x6);

        // 发送请求
        connection.writeRequest(request);

        // 等待响应
        BasicReturns basicReturns = waitResult(queueDeleteArguments.getRid());

        return basicReturns.isOk();
    }

    // 创建绑定
    public boolean queueBind(String queueName, String exchangeName, String bindingKey) throws IOException {
        // 构建参数类对象
        QueueBindArguments queueBindArguments = new QueueBindArguments();
        queueBindArguments.setBindingKey(bindingKey);
        queueBindArguments.setQueueName(queueName);
        queueBindArguments.setExchangeName(exchangeName);
        queueBindArguments.setChannelId(channelId);
        queueBindArguments.setRid(generateRid());

        // 序列化
        byte[] payload = BinaryTool.toBytes(queueBindArguments);

        // 构造请求对象
        Request request = new Request();
        request.setType(0x7);
        request.setLength(payload.length);
        request.setPayload(payload);

        // 发送请求
        connection.writeRequest(request);

        // 等待响应
        BasicReturns basicReturns = waitResult(queueBindArguments.getRid());

        return basicReturns.isOk();
    }

    // 销毁绑定
    public boolean queueUnbind(String exchangeName, String queueName) throws IOException {
        // 构建参数对象
        QueueUnbindArguments queueUnbindArguments = new QueueUnbindArguments();
        queueUnbindArguments.setExchangeName(exchangeName);
        queueUnbindArguments.setQueueName(queueName);
        queueUnbindArguments.setChannelId(channelId);
        queueUnbindArguments.setRid(generateRid());

        // 序列化
        byte[] payload = BinaryTool.toBytes(queueUnbindArguments);

        // 构造请求对象
        Request request = new Request();
        request.setPayload(payload);
        request.setLength(payload.length);
        request.setType(0x8);

        // 发送请求
        connection.writeRequest(request);

        // 等待响应
        BasicReturns basicReturns = waitResult(queueUnbindArguments.getRid());

        return basicReturns.isOk();
    }

    // 发送消息
    public boolean basicPublish(String exchangeName, String routingKey, BasicProperties basicProperties, byte[] body) throws IOException {
        // 构造参数对象
        BasicPublishArguments basicPublishArguments = new BasicPublishArguments();
        basicPublishArguments.setBasicProperties(basicProperties);
        basicPublishArguments.setBody(body);
        basicPublishArguments.setChannelId(channelId);
        basicPublishArguments.setRid(generateRid());
        basicPublishArguments.setExchangeName(exchangeName);
        basicPublishArguments.setRoutingKey(routingKey);

        // 序列化
        byte[] payload = BinaryTool.toBytes(basicPublishArguments);

        // 构造请求对象
        Request request = new Request();
        request.setType(0x9);
        request.setLength(payload.length);
        request.setPayload(payload);

        // 发送请求
        connection.writeRequest(request);

        // 等待请求响应
        BasicReturns basicReturns = waitResult(basicPublishArguments.getRid());

        return basicReturns.isOk();
    }

    // 订阅消息
    public boolean basicConsume( String queueName, boolean autoAck, Consumer consumer) throws MqException, IOException {
        // 判断当前的这个消费者是否有回调函数
        if (this.consumer != null) {
            throw new MqException("该channel已经设置过消费消息的回调函数了，不能重复设置！");
        }
        this.consumer = consumer;

        // 构造参数类对象
        BasicConsumeArguments basicConsumeArguments = new BasicConsumeArguments();
        basicConsumeArguments.setConsumeTag(channelId);
        basicConsumeArguments.setChannelId(channelId);
        basicConsumeArguments.setRid(generateRid());
        basicConsumeArguments.setQueueName(queueName);
        basicConsumeArguments.setAutoAck(autoAck);

        // 序列化
        byte[] payload = BinaryTool.toBytes(basicConsumeArguments);

        // 构造请求对象
        Request request = new Request();
        request.setPayload(payload);
        request.setLength(payload.length);
        request.setType(0xa);

        // 发送请求
        connection.writeRequest(request);

        // 等待响应
        BasicReturns basicReturns = waitResult(basicConsumeArguments.getRid());

        return basicReturns.isOk();

    }

    // 消息应答
    public boolean basicAck(String queueName, String messageId) throws IOException {
        // 构造参数对象
        BasicAckArguments basicAckArguments = new BasicAckArguments();
        basicAckArguments.setChannelId(channelId);
        basicAckArguments.setRid(generateRid());
        basicAckArguments.setQueueName(queueName);
        basicAckArguments.setMessageId(messageId);

        // 序列化
        byte[] payload = BinaryTool.toBytes(basicAckArguments);

        // 构造请求
        Request request = new Request();
        request.setType(0xb);
        request.setLength(payload.length);
        request.setPayload(payload);

        // 发送请求
        connection.writeRequest(request);

        // 等待响应
        BasicReturns basicReturns = waitResult(basicAckArguments.getRid());

        return basicReturns.isOk();

    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public ConcurrentHashMap<String, BasicReturns> getBasicReturnsMap() {
        return basicReturnsMap;
    }

    public void setBasicReturnsMap(ConcurrentHashMap<String, BasicReturns> basicReturnsMap) {
        this.basicReturnsMap = basicReturnsMap;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

}

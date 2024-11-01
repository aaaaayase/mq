package com.yun.mq.mqserver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.yun.mq.common.*;
import com.yun.mq.mqserver.core.BasicProperties;
import org.springframework.http.ResponseEntity;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author yun
 * @date 2024/10/30 19:09
 * @desciption: 消息队列的本体 本质上就是个TCP服务器
 */
public class BrokerServer {

    private ServerSocket serverSocket = null;
    private VirtualHost virtualHost = new VirtualHost("yun-v ");

    // 这个集合用来管理会话
    // key是channelId
    private ConcurrentHashMap<String, Socket> sessions = new ConcurrentHashMap<>();

    private ExecutorService executorService = null;

    // 引入一个变量控制服务器是否继续运行
    private volatile boolean runnable = true;

    public BrokerServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public void start() throws IOException {
        System.out.println("[BrokerServer] 服务器启动！");
        executorService = Executors.newCachedThreadPool();
        try {
            while (runnable) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> {
                    // 让线程池分配线程来处理连接
                    try {
                        processConnection(clientSocket);
                    } catch (ClassNotFoundException | MqException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (SocketException e) {
            System.out.println("[BrokerServer] 服务器停止运行！");
        }
    }

    // 一般来说停止服务器, 就是直接 kill 掉对应进程就行了.
    // 此处还是搞一个单独的停止方法. 主要是用于后续的单元测试.
    public void stop() throws IOException {
        runnable = false;
        // 把线程池中的任务都放弃了. 让线程都销毁.
        executorService.shutdownNow();
        serverSocket.close();
    }

    // 通过这个方法来处理一个个客户端的连接
    private void processConnection(Socket clientSocket) throws ClassNotFoundException, MqException {
        try (InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream()) {
            try (DataInputStream dataInputStream = new DataInputStream(inputStream);
                 DataOutputStream dataOutputStream = new DataOutputStream(outputStream)) {
                while (true) {
                    // 读取请求并解析
                    Request request = readRequest(dataInputStream);
                    // 处理请求并转化为响应
                    Response response = process(request, clientSocket);
                    // 写回响应
                    wirteResponse(response, dataOutputStream);

                }
            } catch (EOFException | SocketException e) {
                // 这里的SocketException异常指的是对端也就是客户端主动去关闭了TCP连接
                System.out.println("[BrokerServer] connection关闭！ 客户端地址：" + clientSocket.getInetAddress().toString() + " 客户端端口号：" + clientSocket.getPort());
            }
        } catch (IOException e) {
            System.out.println("[BrokerServer] connection出现异常！");
            e.printStackTrace();
        } finally {
            try {
                // 当连接处理完了需要关闭连接
                clientSocket.close();
                // 一个TCP连接可能包含多个channel 需要将这些channel全部清理掉
                clearClosedSession(clientSocket);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    // 读取并转化为请求对象
    private Request readRequest(DataInputStream dataInputStream) throws IOException {
        Request request = new Request();
        request.setType(dataInputStream.readInt());
        request.setLength(dataInputStream.readInt());
        byte[] payload = new byte[request.getLength()];
        int n = dataInputStream.read(payload);
        if (n != request.getLength()) {
            throw new IOException("读取请求格式出错！");
        }
        request.setPayload(payload);
        return request;
    }

    // 处理请求并得到响应对象
    private Response process(Request request, Socket clientSocket) throws IOException, ClassNotFoundException, MqException {
        // 先拿到并解析请求的payload
        BasicArguments basicArguments = (BasicArguments) BinaryTool.fromBytes(request.getPayload());
        System.out.println("[Request] rid=" + basicArguments.getRid() + " channelId=" + basicArguments.getChannelId() + " type=" + request.getType() + " length=" + request.getLength());
        // 使用一个布尔类型的变量来记录请求的操作是否成功
        boolean ok = true;
        // 根据type的值来判断这次请求要干嘛
        if (request.getType() == 0x1) {
            // 创建channel
            sessions.put(basicArguments.getChannelId(), clientSocket);
            System.out.println("[BrokerServer] 创建channel完成！channelId=" + basicArguments.getChannelId());
        } else if (request.getType() == 0x2) {
            // 销毁channel
            sessions.remove(basicArguments.getChannelId());
            System.out.println("[BrokerServer] 销毁channel完成！channelId=" + basicArguments.getChannelId());
        } else if (request.getType() == 0x3) {
            // 创建交换机 此时payload就是创建交换机方法ExchangeDeclare方法的参数了 即ExchangeDeclareArguments对象
            ExchangeDeclareArguments arguments = (ExchangeDeclareArguments) basicArguments;
            // 调用相应方法 然后给参数赋值
            ok = virtualHost.exchangeDeclare(arguments.getExchangeName(), arguments.getExchangeType(), arguments.isDurable(), arguments.isAutoDelete(), arguments.getArguments());

        } else if (request.getType() == 0x4) {
            // 删除交换机
            ExchangeDeleteArguments arguments = (ExchangeDeleteArguments) basicArguments;
            // 调用相应方法 然后给参数赋值
            ok = virtualHost.exchangeDelete(arguments.getExchangeName());
        } else if (request.getType() == 0x5) {
            // 创建队列
            QueueDeclareArguments arguments = (QueueDeclareArguments) basicArguments;
            ok = virtualHost.queueDeclare(arguments.getQueueName(), arguments.isDurable(), arguments.isExclusive(), arguments.isAutoDelete(), arguments.getArguments());
        } else if (request.getType() == 0x6) {
            // 删除队列
            QueueDeleteArguments arguments = (QueueDeleteArguments) basicArguments;
            ok = virtualHost.queueDelete(arguments.getQueueName());
        } else if (request.getType() == 0x7) {
            // 创建绑定
            QueueBindArguments arguments = (QueueBindArguments) basicArguments;
            ok = virtualHost.queueBind(arguments.getQueueName(), arguments.getExchangeName(), arguments.getBindingKey());
        } else if (request.getType() == 0x8) {
            // 删除绑定
            QueueUnbindArguments arguments = (QueueUnbindArguments) basicArguments;
            ok = virtualHost.queueUnbind(arguments.getExchangeName(), arguments.getQueueName());
        } else if (request.getType() == 0x9) {
            // 发送消息
            BasicPublishArguments arguments = (BasicPublishArguments) basicArguments;
            ok = virtualHost.basicPublish(arguments.getExchangeName(), arguments.getRoutingKey(), arguments.getBasicProperties(), arguments.getBody());
        } else if (request.getType() == 0xa) {
            // 订阅消息
            BasicConsumeArguments arguments = (BasicConsumeArguments) basicArguments;
            ok = virtualHost.basicConsume(arguments.getConsumeTag(), arguments.getQueueName(), arguments.isAutoAck(), new Consumer() {
                // 这个函数需要做的事情就是将服务器收到的消息给发到对应的消费者客户端
                @Override
                public void handleDelivery(String consumerTag, BasicProperties basicProperties, byte[] body) throws IOException, MqException {
                    // 先知道要发那个客户端
                    // 再从sessions中通过客户端名取到socket连接
                    // 再从socket中传输数据
                    // 1. 首先从通过channelId来获取socket对象 这里的channelId就是consumerTag 因为消费者就是客户端（可以这么理解）
                    Socket clientSocket = sessions.get(consumerTag);
                    if (clientSocket == null || clientSocket.isClosed()) {
                        throw new MqException("[BrokerServer] 订阅消息的客户端已经关闭！");
                    }
                    // 2. 构造响应对象
                    Response response = new Response();
                    // 0xc表示服务器给客户端推送的消息数据
                    response.setType(0xc);
                    // 为了设置payload以及length属性 需要去拿到响应的数据内容
                    SubscribeReturns subscribeReturns = new SubscribeReturns();
                    subscribeReturns.setBody(body);
                    subscribeReturns.setConsumerTag(consumerTag);
                    // 因为这里的错误是订阅 这里的逻辑是为了主动将消息交给客户端 所以没有请求 因此配套使用的rid在这里没有用
                    subscribeReturns.setRid("");
                    subscribeReturns.setBasicProperties(basicProperties);
                    subscribeReturns.setOk(true);
                    subscribeReturns.setChannelId(consumerTag);
                    byte[] payload = BinaryTool.toBytes(subscribeReturns);
                    response.setLength(payload.length);
                    response.setPayload(payload);
                    // 3. 将数据写回给客户端
                    DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
                    wirteResponse(response, dataOutputStream);
                }
            });
        } else if (request.getType() == 0xb) {
            // 手动应答
            BasicAckArguments arguments = (BasicAckArguments) basicArguments;
            ok = virtualHost.basicAck(arguments.getQueueName(), arguments.getMessageId());
        } else {
            // 当前的type是非法的
            throw new MqException("[BrokerServer] 未知的type！ type=" + request.getType());
        }

        // 构造响应
        // 当然构造响应之前要先构造好响应的payload
        BasicReturns basicReturns = new BasicReturns();
        basicReturns.setChannelId(basicArguments.getChannelId());
        basicReturns.setOk(ok);
        basicReturns.setRid(basicArguments.getRid());
        Response response = new Response();
        byte[] payload = BinaryTool.toBytes(basicReturns);
        response.setType(request.getType());
        response.setPayload(payload);
        response.setLength(payload.length);
        return response;
    }

    // 将响应写回
    private void wirteResponse(Response response, DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(response.getType());
        dataOutputStream.writeInt(response.getLength());
        dataOutputStream.write(response.getPayload());

        // 刷新缓冲区
        dataOutputStream.flush();

    }


    // 清楚一个TCP连接中的channels
    private void clearClosedSession(Socket clientSocket) {
        // 调用这个方法之前已经关闭了相应的socket连接
        List<String> toDeleteChannelId = new ArrayList<>();
        for (Map.Entry<String, Socket> entry : sessions.entrySet()) {
            if (entry.getValue() == clientSocket) {
                toDeleteChannelId.add(entry.getKey());
            }
        }

        for (String channelId : toDeleteChannelId) {
            sessions.remove(channelId);
        }
        System.out.println("[BrokerServer] 清理session完成！ 被清理的channelId=" + toDeleteChannelId);
    }
}

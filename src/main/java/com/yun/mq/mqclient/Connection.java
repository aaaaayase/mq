package com.yun.mq.mqclient;

import com.yun.mq.common.*;

import java.io.*;
import java.lang.annotation.Repeatable;
import java.net.Socket;
import java.net.SocketException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author yun
 * @date 2024/11/1 9:34
 * @desciption: 表示连接 管理channels
 */
public class Connection {

    private Socket socket = null;

    // 管理逻辑上的通信通道 使用一个集合来管理
    private ConcurrentHashMap<String, Channel> channelMap = new ConcurrentHashMap<>();

    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private DataInputStream dataInputStream = null;
    private DataOutputStream dataOutputStream = null;

    private ExecutorService callbackPool = null;

    public Connection(String host, int port) throws IOException {
        socket = new Socket(host, port);
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
        dataInputStream = new DataInputStream(inputStream);
        dataOutputStream = new DataOutputStream(outputStream);

        callbackPool = Executors.newFixedThreadPool(4);

        // 创建线程来不断地从socket中读取响应数据并放到对应的channel中负责处理
        Thread thread = new Thread(() -> {
            try {
                while (!socket.isClosed()) {
                    Response response = readResponse();
                    dispatchResponse(response);
                }
            } catch (SocketException e) {
                // 此时连接是正常断开的 这个异常可以忽略
                System.out.println("[Connection] 连接正常断开！");
            } catch (IOException | ClassNotFoundException | MqException e) {
                System.out.println("[Connection] 连接异常断开！");
                e.printStackTrace();
            }

        });

        thread.start();

    }

    // 关闭connection 释放资源
    public void close() {
        try {
            callbackPool.shutdownNow();
            channelMap.clear();
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void dispatchResponse(Response response) throws IOException, ClassNotFoundException, MqException {
        if (response.getType() == 0xc) {
            // 服务器推送的消息数据
            SubscribeReturns subscribeReturns = (SubscribeReturns) BinaryTool.fromBytes(response.getPayload());
            Channel channel = channelMap.get(subscribeReturns.getChannelId());
            if (channel == null) {
                throw new MqException("[Connection] 该消息对应的channel在客户端中不存在！ channelId=" + channel.getChannelId());
            }
            // 执行该channel内的回调函数
            callbackPool.submit(() -> {
                try {
                    channel.getConsumer().handleDelivery(subscribeReturns.getConsumerTag(), subscribeReturns.getBasicProperties(), subscribeReturns.getBody());
                } catch (IOException | MqException e) {
                    e.printStackTrace();
                }
            });
        } else {
            // 客户端发送请求的响应
            BasicReturns basicReturns = (BasicReturns) BinaryTool.fromBytes(response.getPayload());
            // 把这个结果放到对应的channel的集合中
            Channel channel = channelMap.get(basicReturns.getChannelId());
            if (channel == null) {
                throw new MqException("[Connection] 该消息对应的channel在客户端中不存在！ channelId=" + channel.getChannelId());
            }
            channel.putReturns(basicReturns);
        }
    }

    // 发送消息
    public void writeRequest(Request request) throws IOException {
        dataOutputStream.writeInt(request.getType());
        dataOutputStream.writeInt(request.getLength());
        dataOutputStream.write(request.getPayload());
        dataOutputStream.flush();
        System.out.println("[Connection] 发送请求！ type=" + request.getType() + " length=" + request.getLength());
    }

    // 读取响应
    public Response readResponse() throws IOException {
        Response response = new Response();
        response.setType(dataInputStream.readInt());
        response.setLength(dataInputStream.readInt());
        byte[] payload = new byte[response.getLength()];
        int n = dataInputStream.read(payload);
        if (n != response.getLength()) {
            throw new IOException("读取的数据不完整！");
        }
        response.setPayload(payload);
        System.out.println("[Connection] 收到响应！ type=" + response.getType() + " length=" + response.getLength());
        return response;
    }

    // 创建channel
    public Channel createChannel() throws IOException {
        // 使用UUID生成通道id
        String channelId = "C-" + UUID.randomUUID().toString();
        // 创建channel
        Channel channel = new Channel(channelId, this);
        // 将channel对象放到集合中进行管理
        channelMap.put(channelId, channel);
        // 同时也需要将创建channel这个消息来告诉服务器
        boolean ok = channel.createChannel();
        if (!ok) {
            // 服务器那边的逻辑失败了
            // 因此要回滚之前的操作
            // 删除在集合中存放的通道
            channelMap.remove(channelId);
            return null;
        }
        return channel;
    }
}

package com.yun.mq.mqserver;

import com.yun.mq.common.Request;
import com.yun.mq.common.Response;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
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
    private VirtualHost virtualHost = new VirtualHost("default");

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
        while (runnable) {
            Socket clientSocket = serverSocket.accept();
            executorService.submit(() -> {
                // 让线程池分配线程来处理连接
                processConnection(clientSocket);
            });
        }
    }

    // 通过这个方法来处理一个个客户端的连接
    private void processConnection(Socket clientSocket) {
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
    private Response process(Request request, Socket clientSocket) {

        return null;
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
    }
}

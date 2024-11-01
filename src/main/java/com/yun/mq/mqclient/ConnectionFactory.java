package com.yun.mq.mqclient;

import java.io.IOException;

/**
 * @author yun
 * @date 2024/11/1 9:32
 * @desciption: 管理Socket连接
 */
public class ConnectionFactory {

    // broker server服务器ip
    private String host;
    // broker server服务器端口
    private int port;

    // 控制访问broker server的哪个虚拟主机
    // 下列几个属性先不搞
//    private String virtualHostName;
//    private String userName;
//    private String password;

    public Connection newConnection() {
        Connection connection = null;
        try {
            connection = new Connection(host, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}

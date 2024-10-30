package com.yun.mq.common;


import com.yun.mq.mqserver.core.BasicProperties;

import java.io.Serializable;

/**
 * @author yun
 * @date 2024/10/30 18:48
 * @desciption: 发布消息参数类
 */
public class BasicPublishArguments extends BasicArguments implements Serializable {
    private String exchangeName;
    private String routingKey;
    private BasicProperties basicProperties;
    private byte[] body;

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public BasicProperties getBasicProperties() {
        return basicProperties;
    }

    public void setBasicProperties(BasicProperties basicProperties) {
        this.basicProperties = basicProperties;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}

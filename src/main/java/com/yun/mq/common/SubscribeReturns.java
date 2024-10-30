package com.yun.mq.common;

import com.yun.mq.mqserver.core.BasicProperties;

import java.io.Serializable;

/**
 * @author yun
 * @date 2024/10/30 19:04
 * @desciption: 订阅消息成功后返回
 */
public class SubscribeReturns extends BasicReturns implements Serializable {
    private String consumerTag;
    private BasicProperties basicProperties;
    private byte[] body;

    public String getConsumerTag() {
        return consumerTag;
    }

    public void setConsumerTag(String consumerTag) {
        this.consumerTag = consumerTag;
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

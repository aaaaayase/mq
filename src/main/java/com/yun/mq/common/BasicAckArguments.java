package com.yun.mq.common;


import java.io.Serializable;

/**
 * @author yun
 * @date 2024/10/30 19:02
 * @desciption: 手动应答参数类
 */
public class BasicAckArguments extends BasicArguments implements Serializable {
    private String queueName;
    private String messageId;

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}

package com.yun.mq.common;

import java.io.Serializable;

/**
 * @author yun
 * @date 2024/10/30 18:46
 * @desciption: 解除绑定参数类
 */
public class queueUnbindArguments extends BasicArguments implements Serializable {

    private String exchangeName;
    private String queueName;

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }
}

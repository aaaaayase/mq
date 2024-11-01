package com.yun.mq.common;

import java.io.Serializable;

/**
 * @author yun
 * @date 2024/10/30 18:43
 * @desciption: 删除队列参数类
 */
public class QueueDeleteArguments extends BasicArguments implements Serializable {
    private String queueName;

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }
}

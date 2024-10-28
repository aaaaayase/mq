package com.yun.mq.common;

import com.yun.mq.mqserver.core.BasicProperties;

/**
 * @author yun
 * @date 2024/10/28 15:57
 * @desciption: 消费者相关函数式接口
 */
public interface Consumer {

    // 服务器收到消息 使用此方法将消息推送给相应的订阅用户
    void handleDelivery(String consumerTag, BasicProperties basicProperties, byte[] body);
}

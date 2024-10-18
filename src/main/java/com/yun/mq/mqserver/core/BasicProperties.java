package com.yun.mq.mqserver.core;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author yun
 * @date 2024/10/18 20:25
 * @desciption: 消息属性
 */
@Setter
@Getter
public class BasicProperties implements Serializable {

    // 消息的标识 使用UUID来生成
    private String messageId;

    // 当交换机类型为DIRECT 会将消息放到routingKey指定的队列
    // FANOUT时就不管routingKey的指向
    // TOPIC时就会找到于routingKey相同的bindingKey 然后根据binding的关系将消息给相应的队列
    private String routingKey;

    // 这个属性表示消息是否需要持久化 1表示不需要持久化 2表示持久化
    private int deliverMode=1;

    // 其它属性暂不去考虑

}

package com.yun.mq.mqserver.core;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yun
 * @date 2024/10/18 19:26
 * @desciption: 存储消息的队列类
 */
@Getter
@Setter
public class MSGQueue {

    // 作为队列标识
    private String name;

    // 作为是否持久化存储的依据
    private boolean durable=false;

    // true表示只能被一个消费者示使用 false表示大家都能使用
    private boolean exclusive=false;

    // true时没人使用时自动删除队列
    private boolean autoDelete=false;

    private Map<String,Object> arguments=new HashMap<>();
}

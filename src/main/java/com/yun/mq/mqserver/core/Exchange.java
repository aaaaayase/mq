package com.yun.mq.mqserver.core;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author yun
 * @date 2024/10/18 19:26
 * @desciption: 交换机类
 */
@Getter
@Setter
public class Exchange {

    // 类似于平时使用的id作为交换机的标识 rabbitmq中也是使用name
    private String name;

    // 交换机的类型  DIRECT FANOUT TOPIC
    private ExchangeType exchangeType=ExchangeType.DIRECT;

    // 用于判断是否需要持久化
    private boolean durable=false;

    // 如果当前交换机没人使用了就会自动删除（指的没有生产者使用）
    //
    private boolean autoDelete=false;
    // 额外的可选参数
    private Map<String, Object> arguments=new HashMap<>();


}

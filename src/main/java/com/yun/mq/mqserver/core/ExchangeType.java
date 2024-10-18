package com.yun.mq.mqserver.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yun
 * @date 2024/10/18 19:41
 * @desciption: 交换机类型枚举类
 */
@AllArgsConstructor
@Getter
public enum ExchangeType {

    DIRECT(0),
    FANOUT(1),
    TOPIC(2);

    private final int type;


}

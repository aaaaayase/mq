package com.yun.mq.mqserver.core;

/**
 * @author yun
 * @date 2024/10/18 19:41
 * @desciption: 交换机类型枚举类
 */
public enum ExchangeType {

    DIRECT(0),
    FANOUT(1),
    TOPIC(2);

    private final int type;

    ExchangeType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}

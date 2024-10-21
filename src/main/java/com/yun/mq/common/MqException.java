package com.yun.mq.common;

/**
 * @author yun
 * @date 2024/10/21 15:12
 * @desciption: 自定义消息队列异常
 */
public class MqException extends Exception {

    public MqException(String reason) {
        super(reason);
    }

}

package com.yun.mq.common;


import java.io.Serializable;

/**
 * @author yun
 * @date 2024/10/30 18:50
 * @desciption: 订阅消息参数类
 */
public class BasicConsumeArguments extends BasicArguments implements Serializable {

    private String consumeTag;
    private String queueName;
    private boolean autoAck;
    // 在订阅消息这个方法中还有一个参数是函数式接口
    // 我们客户端是通过网络来传递数据 是无法将这样的回调函数传过来然后让这里的参数接收的
    // 因此我们这里就没有Consumer这样的属性
    // 实际上我们也并不需要这样的属性
    // 因为在mq的设计中 对于服务器消息订阅的回调函数会被设置为固定的将消息交给消费者的逻辑
    // 至于消费者也就是客户端本身要对消息做什么那就交给它自己决定 可以它们自己在客户端实现一套逻辑


    public String getConsumeTag() {
        return consumeTag;
    }

    public void setConsumeTag(String consumeTag) {
        this.consumeTag = consumeTag;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public boolean isAutoAck() {
        return autoAck;
    }

    public void setAutoAck(boolean autoAck) {
        this.autoAck = autoAck;
    }
}

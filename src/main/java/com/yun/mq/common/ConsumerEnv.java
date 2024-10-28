package com.yun.mq.common;

/**
 * @author yun
 * @date 2024/10/28 18:47
 * @desciption: 表示一个消费者
 */
public class ConsumerEnv {

    private String consumerTag;

    private String queueName;

    private boolean autoAck;

    // 通过这个回调来处理收到的消息
    private Consumer consumer;

    public ConsumerEnv(String consumerTag, String queueName, boolean autoAck, Consumer consumer) {
        this.consumerTag = consumerTag;
        this.queueName = queueName;
        this.autoAck = autoAck;
        this.consumer = consumer;
    }

    public String getConsumerTag() {
        return consumerTag;
    }

    public void setConsumerTag(String consumerTag) {
        this.consumerTag = consumerTag;
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

    public Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }
}

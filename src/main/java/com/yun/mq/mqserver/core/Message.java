package com.yun.mq.mqserver.core;


import java.io.Serializable;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.stream.IntStream;

/**
 * @author yun
 * @date 2024/10/18 19:26
 * @desciption: 消息类 表示一个要传递的信息
 *
 * 因为message是需要在网络上进行传输并且也要能够写到文件中
 * 需要对message进行序列化以及反序列化
 * 此处使用标准库自带的序列化/反序列化
 */

public class Message implements Serializable {

    // 这两个属性时最核心的部分 其它属性用于辅助
    private BasicProperties basicProperties=new BasicProperties();

    private byte[] body;

    // 以下是辅助属性
    // 消息想要进行持久化存储需要保存在文件中
    // 在一个文件中需要保存多个消息 此时就需要去指定每一条消息的起始位置
    // 这里采用了左闭右开的常用方式
    // 这里的两个属性不需要去序列化 因为消息写入文件位置就固定了 不需要去单独存储
    // 另外这两属性的用处就是为了让内存中的message对象找到对应硬盘上message的位置
    private transient long offsetBeg=0;
    private transient long offsetEnd=0;

    // 文件中的消息的删除采用的是逻辑删除的方法 因此需要以下属性去指定当前的属性去当前的消息是否是有效的
    // 0x1表示有效 0x0表示无效
    private byte isValid=0x1;

    // 添加一些便于操作的方法
    public void setMessageId(String messageId) {
        basicProperties.setMessageId(messageId);
    }

    public String getMessageId() {
        return basicProperties.getMessageId();
    }

    public void setDeliverMode(int deliverMode) {
        basicProperties.setDeliverMode(deliverMode);
    }

    public int getDeliverMode() {
        return basicProperties.getDeliverMode();
    }


    public void setRoutingKey(String routingKey) {
        basicProperties.setRoutingKey(routingKey);
    }

    public String getRoutingKey() {
        return basicProperties.getRoutingKey();
    }

    // 建立工厂方法返回message对象
    // 方法中不去设置offset以及isValid这些属性 因为这里仅仅是在内存中创建message这样的对象
    // offset这些属性是等到持久化操作时才去设定
    public static Message createMessageWithId(BasicProperties basicProperties,String routingKey,byte[] body) {

        Message message=new Message();
        if(basicProperties!=null) {
            message.setBasicProperties(basicProperties);
        }
        message.setMessageId("M-"+UUID.randomUUID());
        message.setRoutingKey(routingKey);
        message.body=body;

        return message;
    }


    public BasicProperties getBasicProperties() {
        return basicProperties;
    }

    public void setBasicProperties(BasicProperties basicProperties) {
        this.basicProperties = basicProperties;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public long getOffsetBeg() {
        return offsetBeg;
    }

    public void setOffsetBeg(long offsetBeg) {
        this.offsetBeg = offsetBeg;
    }

    public long getOffsetEnd() {
        return offsetEnd;
    }

    public void setOffsetEnd(long offsetEnd) {
        this.offsetEnd = offsetEnd;
    }

    public byte getIsValid() {
        return isValid;
    }

    public void setIsValid(byte isValid) {
        this.isValid = isValid;
    }
}

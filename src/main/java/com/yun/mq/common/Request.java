package com.yun.mq.common;

/**
 * @author yun
 * @date 2024/10/30 15:58
 * @desciption: 表示请求对象
 */
public class Request {
    // 请求类型
    private int type;
    // payload长度 单位为字节
    private int length;

    private byte[] payload;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }
}

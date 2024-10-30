package com.yun.mq.common;

/**
 * @author yun
 * @date 2024/10/30 16:00
 * @desciption: 表示响应对象
 */
public class Response {

    private int type;
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

package com.yun.mq.common;

import java.io.Serializable;

/**
 * @author yun
 * @date 2024/10/30 16:05
 * @desciption: 表示各个远程调用的方法的返回值的公共信息
 */
public class BasicReturns implements Serializable {

    // 表示一次请求或响应的标识
    protected String rid;

    // 每次通信的channel的标识
    protected String channelId;

    // 表示远程调用的方法的返回值
    protected boolean ok;

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }
}

package com.yun.mq.common;

import java.io.Serializable;

/**
 * @author yun
 * @date 2024/10/30 16:01
 * @desciption: 表示一些基础/公共参数 后面不同的参数可以继承这个类的子类来实现
 */
public class BasicArguments implements Serializable {

    // 表示一次请求/响应的身份标识 可以将请求和响应给对上
    protected String rid;

    // 每次通信使用的channel的标识
    protected String channelId;

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
}

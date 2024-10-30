package com.yun.mq.common;

import java.io.Serializable;

/**
 * @author yun
 * @date 2024/10/30 18:40
 * @desciption: 删除交换机参数类
 */
public class ExchangeDeleteArguments extends BasicArguments implements Serializable {
    private String exchangeName;

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }
}

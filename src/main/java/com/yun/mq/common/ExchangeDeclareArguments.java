package com.yun.mq.common;

import com.yun.mq.mqserver.core.ExchangeType;

import java.io.Serializable;
import java.util.Map;

/**
 * @author yun
 * @date 2024/10/30 16:25
 * @desciption: 创建交换机参数类
 */
public class ExchangeDeclareArguments extends BasicArguments implements Serializable {

    private String exchangeName;
    private ExchangeType exchangeType;
    private boolean durable;
    private boolean autoDelete;
    private Map<String, Object> arguments;

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public ExchangeType getExchangeType() {
        return exchangeType;
    }

    public void setExchangeType(ExchangeType exchangeType) {
        this.exchangeType = exchangeType;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }

    public boolean isAutoDelete() {
        return autoDelete;
    }

    public void setAutoDelete(boolean autoDelete) {
        this.autoDelete = autoDelete;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}

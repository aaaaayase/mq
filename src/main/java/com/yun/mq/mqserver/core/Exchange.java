package com.yun.mq.mqserver.core;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author yun
 * @date 2024/10/18 19:26
 * @desciption: 交换机类
 */
public class Exchange {

    // 类似于平时使用的id作为交换机的标识 rabbitmq中也是使用name
    private String name;

    // 交换机的类型  DIRECT FANOUT TOPIC
    private ExchangeType type = ExchangeType.DIRECT;

    // 用于判断是否需要持久化
    private boolean durable = false;

    // 如果当前交换机没人使用了就会自动删除（指的没有生产者使用）
    //
    private boolean autoDelete = false;
    // 额外的可选参数
    // 为了将键值对存入数据库就需要将其转为json格式的字符串
    private Map<String, Object> arguments = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ExchangeType getExchangeType() {
        return type;
    }

    public void setExchangeType(ExchangeType exchangeType) {
        this.type = exchangeType;
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

    // 把hash类型转为字符串类型
    public String getArguments() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(arguments);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    // 读取数据库 将字符串转为hash类型
    public void setArguments(String argumentsJson) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            this.arguments = objectMapper.readValue(argumentsJson, new TypeReference<HashMap<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    // 为了便于测试 再写一组针对arguments的get和set方法 只用于java内部代码使用
    public Object getArguments(String key) {
        return arguments.get(key);
    }

    public void setArguments(String key, Object object) {
        arguments.put(key, object);
    }
}

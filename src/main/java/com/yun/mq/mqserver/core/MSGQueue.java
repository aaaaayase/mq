package com.yun.mq.mqserver.core;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yun
 * @date 2024/10/18 19:26
 * @desciption: 存储消息的队列类
 */
public class MSGQueue {

    // 作为队列标识
    private String name;

    // 作为是否持久化存储的依据
    private boolean durable = false;

    // true表示只能被一个消费者示使用 false表示大家都能使用
    private boolean exclusive = false;

    // true时没人使用时自动删除队列
    private boolean autoDelete = false;

    private Map<String, Object> arguments = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
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

    // 为了方便单元测试提供另外一组get和set函数
    public Object getArguments(String key) {
        return arguments.get(key);
    }

    public void setArguments(String key, Object object) {
        arguments.put(key, object);
    }
}

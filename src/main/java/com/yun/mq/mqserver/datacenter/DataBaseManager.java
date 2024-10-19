package com.yun.mq.mqserver.datacenter;

import com.yun.mq.MqApplication;
import com.yun.mq.mqserver.core.Binding;
import com.yun.mq.mqserver.core.Exchange;
import com.yun.mq.mqserver.core.ExchangeType;
import com.yun.mq.mqserver.core.MSGQueue;
import com.yun.mq.mqserver.mapper.MetaMapper;


import java.io.File;
import java.util.List;

/**
 * @author yun
 * @date 2024/10/19 12:55
 * @desciption: 通过这个类来整合数据库操作
 */
public class DataBaseManager {

    private MetaMapper metaMapper;

    // 初始化数据库
    public void init() {
        // 获取metaMapper对象
        this.metaMapper = MqApplication.context.getBean(MetaMapper.class);

        // 首先判断数据库是否存在
        if (!checkExists()) {
            // 数据库不存在
            // 先创建一个data目录
            File dataDir = new File("./data");
            dataDir.mkdirs();
            // 建表
            //
            createTable();
            // 创建默认的数据
            createDefaultData();
            System.out.println("[DataBaseManager] 数据库初始化完成！");

        } else {
            // 数据库已经存在了 什么都不需要去做
            System.out.println("[DataBaseManager] 数据库已经存在！");
        }

    }

    // 删除数据库
    public void deleteDB() {
        File file = new File("./data/meta.db");
        if (file.delete()) {
            System.out.println("[DataBaseManager] 数据库删除成功！");
        } else {
            System.out.println("[DataBaseManager] 数据库删除失败！");
        }
        // 删完文件继续删除目录
        File dataDir = new File("./data");
        if (dataDir.delete()) {
            System.out.println("[DataBaseManager] data目录删除成功");
        } else {
            System.out.println("[DataBaseManager] data目录删除失败");
        }

    }

    // 判断数据库是否存在
    private boolean checkExists() {
        File file = new File("./data/meta.db");
        if (file.exists()) {
            return true;
        }
        return false;
    }

    // 这里不需要去先建库再建表 因为当进行数据库操作的时候会自动将库给建好
    private void createTable() {
        metaMapper.createBindingTable();
        metaMapper.createQueueTable();
        metaMapper.createExchangeTable();
        System.out.println("[DataBaseManager] 创建表完成！");
    }

    // rabbitmq当中有一个设定就是带有一个默认的交换机 类型是direct
    private void createDefaultData() {
        Exchange exchange = new Exchange();
        exchange.setName("");
        exchange.setExchangeType(ExchangeType.DIRECT);
        exchange.setDurable(true);
        exchange.setAutoDelete(false);
        metaMapper.insertExchange(exchange);

        System.out.println("[DataBaseManager] 创建初始数据完成！");
    }

    // 封装数据库操作
    public void insertExchange(Exchange exchange) {
        metaMapper.insertExchange(exchange);
    }

    public void deleteExchange(String exchangeName) {
        metaMapper.deleteExchange(exchangeName);
    }

    public List<Exchange> selectAllExchanges() {
        return metaMapper.selectAllExchanges();
    }

    public void insertQueue(MSGQueue queue) {
        metaMapper.insertQueue(queue);
    }

    public void deleteQueue(String queueName) {
        metaMapper.deleteQueue(queueName);
    }

    public List<MSGQueue> selectAllQueues() {
        return metaMapper.selectAllQueues();
    }

    public void insertBinding(Binding binding) {
        metaMapper.insertBinding(binding);
    }

    public void deleteBinding(Binding binding) {
        metaMapper.deleteBinding(binding);
    }

    public List<Binding> selectAllBindings() {
        return metaMapper.selectAllBindings();
    }

}

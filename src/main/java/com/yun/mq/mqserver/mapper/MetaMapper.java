package com.yun.mq.mqserver.mapper;

import com.yun.mq.mqserver.core.Binding;
import com.yun.mq.mqserver.core.Exchange;
import com.yun.mq.mqserver.core.MSGQueue;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author yun
 * @date 2024/10/19 8:57
 * @desciption: 元数据数据访问接口
 */
@Mapper
public interface MetaMapper {

    // 提供三个核心类的建表操作
    void createExchangeTable();
    void createBindingTable();
    void createQueueTable();

    // 为上面的三个基本概念提供插入和删除基本操作
    void insertExchange(Exchange exchange);
    void deleteExchange(String exchangeName);
    List<Exchange> selectAllExchanges();

    void insertBinding(Binding binding);
    void deleteBinding(Binding binding);
    List<Binding> selectAllBindings();

    void insertQueue(MSGQueue queue);
    void deleteQueue(String queueName);
    List<MSGQueue> selectAllQueues();

}

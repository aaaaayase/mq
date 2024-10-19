package com.yun.mq.mqserver.datacenter;

import com.yun.mq.MqApplication;
import com.yun.mq.mqserver.core.Binding;
import com.yun.mq.mqserver.core.Exchange;
import com.yun.mq.mqserver.core.ExchangeType;
import com.yun.mq.mqserver.core.MSGQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yun
 * @date 2024/10/19 15:07
 * @desciption:
 */
@SpringBootTest
class DataBaseManagerTest {

    DataBaseManager dataBaseManager = new DataBaseManager();

    // 接下来要编写多个方法来进行测试
    // 需要进行准备工作 编写两个方法分别用于进行“准备工作”以及“收尾工作”
    @BeforeEach
    public void setUp() {
        // 由于再init方法中需要通过context对象拿到MetaMapper实例 所以要先准备context对象
        MqApplication.context = SpringApplication.run(MqApplication.class);
        dataBaseManager.init();
    }

    // 收尾工作
    @AfterEach
    public void tearDown() {
        // 这里要做的就是将数据库给清空
        // 但是这里不能直接删除 要先关闭context对象 因为context对象持有了MetaMapper实例 MetaMapper实例打开meta.db 在windows下打开一个文件的情况下无法删除该文件
        // 另外就是context把8080端口占了 close也是可以释放该端口
        MqApplication.context.close();
        dataBaseManager.deleteDB();
    }

    @Test
    public void testInitTable() {
        List<Exchange> exchangeList = dataBaseManager.selectAllExchanges();
        List<Binding> bindingList = dataBaseManager.selectAllBindings();
        List<MSGQueue> queueList = dataBaseManager.selectAllQueues();

        Assertions.assertEquals(1, exchangeList.size());
        Assertions.assertEquals("", exchangeList.get(0).getName());
        Assertions.assertEquals(ExchangeType.DIRECT, exchangeList.get(0).getExchangeType());
        Assertions.assertEquals(0, bindingList.size());
        Assertions.assertEquals(0, queueList.size());
    }

    private Exchange createTestExchange(String exchangeName) {
        Exchange exchange = new Exchange();
        exchange.setName(exchangeName);
        exchange.setAutoDelete(false);
        exchange.setDurable(true);
        exchange.setExchangeType(ExchangeType.FANOUT);
        exchange.setArguments("aaa", "111");
        exchange.setArguments("bbb", "222");
        return exchange;
    }

    @Test
    public void testInsertExchange() {
        Exchange exchange = createTestExchange("first");
        dataBaseManager.insertExchange(exchange);
        List<Exchange> exchangeList = dataBaseManager.selectAllExchanges();
        assertEquals(2, exchangeList.size());
        assertEquals("first", exchangeList.get(1).getName());
        assertEquals(ExchangeType.FANOUT, exchangeList.get(1).getExchangeType());
        assertEquals(false, exchangeList.get(1).isAutoDelete());
        assertEquals(true, exchangeList.get(1).isDurable());
        assertEquals("111", exchangeList.get(1).getArguments("aaa"));
        assertEquals("222", exchangeList.get(1).getArguments("bbb"));

    }

    @Test
    public void testDeleteExchange() {
        Exchange exchange = createTestExchange("yun");
        dataBaseManager.insertExchange(exchange);
        List<Exchange> exchangeList = dataBaseManager.selectAllExchanges();
        Assertions.assertEquals(2, exchangeList.size());
        Assertions.assertEquals("yun", exchangeList.get(1).getName());
        dataBaseManager.deleteExchange("yun");
        exchangeList = dataBaseManager.selectAllExchanges();
        Assertions.assertEquals("", exchangeList.get(0).getName());
        Assertions.assertEquals(1, exchangeList.size());

    }

    private MSGQueue createTestQueue() {
        MSGQueue queue = new MSGQueue();
        queue.setName("yun");
        queue.setDurable(true);
        queue.setAutoDelete(false);
        queue.setExclusive(false);
        queue.setArguments("aaa", "111");
        queue.setArguments("bbb", "222");
        return queue;
    }

    @Test
    public void testInsertQueue() {
        MSGQueue queue = createTestQueue();
        dataBaseManager.insertQueue(queue);
        List<MSGQueue> queueList = dataBaseManager.selectAllQueues();
        Assertions.assertEquals(1, queueList.size());
        Assertions.assertEquals("yun", queueList.get(0).getName());
        Assertions.assertEquals(false, queueList.get(0).isAutoDelete());
        Assertions.assertEquals(false, queueList.get(0).isExclusive());
        Assertions.assertEquals(true, queueList.get(0).isDurable());
        Assertions.assertEquals("111", queueList.get(0).getArguments("aaa"));
        Assertions.assertEquals("222", queueList.get(0).getArguments("bbb"));
    }

    @Test
    public void testDeleteQueue() {
        MSGQueue queue = createTestQueue();
        dataBaseManager.insertQueue(queue);
        List<MSGQueue> queueList = dataBaseManager.selectAllQueues();
        dataBaseManager.deleteQueue("yun");
        Assertions.assertEquals(1, queueList.size());
        queueList = dataBaseManager.selectAllQueues();
        Assertions.assertEquals(0, queueList.size());
    }

    private Binding createTestBinding() {
        Binding binding = new Binding();
        binding.setBindingKey("a");
        binding.setExchangeName("b");
        binding.setQueueName("c");

        return binding;
    }

    @Test
    public void testInsertBinding() {
        Binding binding = createTestBinding();
        dataBaseManager.insertBinding(binding);
        List<Binding> bindingList = dataBaseManager.selectAllBindings();
        Assertions.assertEquals(1,bindingList.size());
        Assertions.assertEquals("a",bindingList.get(0).getBindingKey());
        Assertions.assertEquals("b",bindingList.get(0).getExchangeName());
        Assertions.assertEquals("c",bindingList.get(0).getQueueName());
    }

    @Test
    public void testDeleteBinding() {
        Binding binding=createTestBinding();
        dataBaseManager.insertBinding(binding);
        List<Binding> bindingList = dataBaseManager.selectAllBindings();
        Assertions.assertEquals(1,bindingList.size());
        dataBaseManager.deleteBinding(binding);
        bindingList=dataBaseManager.selectAllBindings();
        Assertions.assertEquals(0,bindingList.size());

    }
}


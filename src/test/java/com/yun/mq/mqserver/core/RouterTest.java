package com.yun.mq.mqserver.core;

import com.yun.mq.common.MqException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.RouteMatcher;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yun
 * @date 2024/10/28 15:29
 * @desciption:
 */
@SpringBootTest
public class RouterTest {
    private Router router = new Router();

    private Binding binding = null;

    private Message message = null;

    @BeforeEach
    public void setUp() {
        binding = new Binding();
        message = new Message();
    }

    @AfterEach
    public void tearDown() {
        binding = null;
        message = null;
    }

    @Test
    public void test1() throws MqException {
        binding.setBindingKey("aaa.bbb");
        message.setRoutingKey("aaa.bbb");

        Assertions.assertEquals(true, router.route(ExchangeType.TOPIC, binding, message));
    }

    @Test
    public void test2() throws MqException {
        binding.setBindingKey("aaa.bb.c");
        message.setRoutingKey("aaa.bb");
        Assertions.assertEquals(false, router.route(ExchangeType.TOPIC, binding, message));
    }

    @Test
    public void test3() throws MqException {
        binding.setBindingKey("aaa.c");
        message.setRoutingKey("aaa.bb");
        Assertions.assertEquals(false, router.route(ExchangeType.TOPIC, binding, message));
    }

    @Test
    public void test4() throws MqException {
        binding.setBindingKey("aaa.*");
        message.setRoutingKey("aaa.bb");
        Assertions.assertEquals(true, router.route(ExchangeType.TOPIC, binding, message));
    }

    @Test
    public void test5() throws MqException {
        binding.setBindingKey("*.aaa.bb");
        message.setRoutingKey("aaa.bb");
        Assertions.assertFalse(router.route(ExchangeType.TOPIC, binding, message));
    }

    @Test
    public void test6() throws MqException {
        binding.setBindingKey("#");
        message.setRoutingKey("aaa.bb");
        Assertions.assertTrue(router.route(ExchangeType.TOPIC, binding, message));
    }
    @Test
    public void test7() throws MqException {
        binding.setBindingKey("aaa.#");
        message.setRoutingKey("aaa.bb");
        Assertions.assertTrue(router.route(ExchangeType.TOPIC, binding, message));
    }
    @Test
    public void test8() throws MqException {
        binding.setBindingKey("aaa.#");
        message.setRoutingKey("aaa.bb.c");
        Assertions.assertTrue(router.route(ExchangeType.TOPIC, binding, message));
    }

    @Test
    public void test9() throws MqException {
        binding.setBindingKey("aaa.#.c");
        message.setRoutingKey("aaa.c");
        Assertions.assertTrue(router.route(ExchangeType.TOPIC, binding, message));
    }
    @Test
    public void test10() throws MqException {
        binding.setBindingKey("aaa.#.c");
        message.setRoutingKey("aaa.b");
        Assertions.assertFalse(router.route(ExchangeType.TOPIC, binding, message));
    }




}
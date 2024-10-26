package com.yun.mq.mqserver.core;

import com.yun.mq.common.MqException;

/**
 * @author yun
 * @date 2024/10/25 13:19
 * @desciption: 使用这个类来实现交换机转发规则 同时也借助这个类来验证bindingKey合法性
 */
public class Router {

    public boolean checkBindingKey(String bindingKey) {
        // TODO
        return true;
    }

    public boolean checkRoutingKey(String routingKey) {
        // TODO
        return true;
    }

    // 这个方法用来检验消息是否能够发给绑定指定的队列
    public boolean route(ExchangeType exchangeType, Binding binding, Message message) throws MqException {
        //  根据不同的交换机类型来使用不同的规则
        if (exchangeType == ExchangeType.FANOUT) {
            return true;
        } else if (exchangeType == ExchangeType.TOPIC) {
            // 如果是topic主题交换机 规则就更复杂一些
            return routeTopic(binding, message);
        } else {
            // 其它情况不应该存在
            throw new MqException("[Router] 交换机类型非法！ exchangeType=" + exchangeType);
        }
    }

    private boolean routeTopic(Binding binding, Message message) {

        return true;
    }
}

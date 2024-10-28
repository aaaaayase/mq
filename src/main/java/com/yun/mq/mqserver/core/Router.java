package com.yun.mq.mqserver.core;

import com.yun.mq.common.MqException;
import org.apache.ibatis.annotations.Param;

/**
 * @author yun
 * @date 2024/10/25 13:19
 * @desciption: 使用这个类来实现交换机转发规则 同时也借助这个类来验证bindingKey合法性
 */
public class Router {

    // bindingKey构造的规则
    // 1. 使用字母数字以及下划线
    // 2. 使用.分割
    // 3. 使用*以及#作为分隔符
    public boolean checkBindingKey(String bindingKey) {
        if (bindingKey.length() == 0) {
            // bindingKey长度为0这种情况是合法的
            // 使用直接交换机时 因为按照routingKey直接指定队列名所以不需要bindingKey
            // 使用扇出交换机时 因为交换机相应绑定的队列都要转发所以也不在乎bindingKey
            return true;
        }

        for (int i = 0; i < bindingKey.length(); i++) {
            char ch = bindingKey.charAt(i);
            if (ch >= 'a' && ch <= 'z') {
                continue;
            }

            if (ch >= 'A' && ch <= 'Z') {
                continue;
            }

            if (ch >= '0' && ch <= '9') {
                continue;
            }

            if (ch == '.' || ch == '_' || ch == '*' || ch == '#') {
                continue;
            }
            return false;
        }

        // 分割bindingKey帮助校验* #，这两个字符是不能够和其它字符组合作为一个部分
        String[] words = bindingKey.split("\\.");
        for (String word : words) {
            if (word.length() > 1 && (word.contains("*") || word.contains("#"))) {
                return false;
            }
        }

        // 除了以上还要校验一些规则
        // 1. .#.#. 不合法
        // 2. .#.*. 不合法
        // 3. .*.#. 不合法
        // 4. .*.*. 合法

        for (int i = 0; i < words.length - 1; i++) {
            if (words[i].equals("#") && words[i].equals(words[i + 1])) {
                return false;
            }
            if (words[i].equals("#") && words[i + 1].equals("*")) {
                return false;
            }
            if (words[i].equals("*") && words[i + 1].equals("#")) {
                return false;
            }
        }
        return true;
    }

    // routingKey构造的规则：
    // 1. 使用字母数字以及下划线
    // 2. 使用.进行分割
    public boolean checkRoutingKey(String routingKey) {
        if (routingKey.length() == 0) {
            // routingKey为空字符串这样的情况是可以被允许的 因为此时就代表着交换机绑定的所有队列消息都要发送
            // 例如使用fanout交换机的时候 routingKey用不上
            return true;
        }
        for (int i = 0; i < routingKey.length(); i++) {
            char ch = routingKey.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                continue;
            }

            if (ch >= 'a' && ch <= 'z') {
                continue;
            }

            if (ch >= '0' && ch <= '9') {
                continue;
            }

            if (ch == '_' || ch == '.') {
                continue;
            }

            // 如果以上条件都不满足 就说明不满足routingKey的规则 因此返沪false
            return false;
        }

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
        String[] bindingTokens = binding.getBindingKey().split("\\.");
        String[] routingTokens = message.getRoutingKey().split("\\.");

        int bindingIndex = 0;
        int routingIndex = 0;

        // 这里使用双指针的方法
        while (bindingIndex < bindingTokens.length && routingIndex < routingTokens.length) {
            // 走到*可以匹配任意的一部分 所以两个指针都向后走一位
            if (bindingTokens[bindingIndex].equals("*")) {
                // [情况1] 遇到*
                bindingIndex++;
                routingIndex++;
                continue;
            } else if (bindingTokens[bindingIndex].equals("#")) {
                bindingIndex++;
                if (bindingIndex == bindingTokens.length) {
                    // [情况2] 遇到# 并且#后无字符
                    return true;
                }
                // [情况3] 遇到# 并且#后有字符
                routingIndex = findNextMatch(bindingTokens[bindingIndex], routingIndex, routingTokens);
                if (routingIndex == -1) {
                    return false;
                }

                // 找到了的匹配情况
                bindingIndex++;
                routingIndex++;
            } else {
                // [情况4] 遇到正常的字符串
                if (!bindingTokens[bindingIndex].equals(routingTokens[routingIndex])) {
                    return false;
                }
                bindingIndex++;
                routingIndex++;

            }


        }

        // [情况5] 遍历完成 判断两个字符串是否都走到最终
        if (bindingIndex == bindingTokens.length && routingIndex == routingTokens.length) {
            return true;
        }

        return false;
    }

    private int findNextMatch(String bindingToken, int routingIndex, String[] routingTokens) {
        for (int i = routingIndex; i < routingTokens.length; i++) {
            if (routingTokens[i].equals(bindingToken)) {
                return i;
            }
        }

        return -1;
    }


}

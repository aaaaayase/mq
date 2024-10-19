package com.yun.mq.mqserver.core;


/**
 * @author yun
 * @date 2024/10/18 19:26
 * @desciption: 绑定关系类（交换机和消息队列之间的关系）
 */
public class Binding {

    // 消息队列标识
   private String queueName;

   // 交换机标识
   private String exchangeName;

   // 用于主题交换机
   private String bindingKey;

   // 因为binding是依存于队列以及交换机的
   // 所以这里设定例如durable是没有意义的


   public String getQueueName() {
      return queueName;
   }

   public void setQueueName(String queueName) {
      this.queueName = queueName;
   }

   public String getExchangeName() {
      return exchangeName;
   }

   public void setExchangeName(String exchangeName) {
      this.exchangeName = exchangeName;
   }

   public String getBindingKey() {
      return bindingKey;
   }

   public void setBindingKey(String bindingKey) {
      this.bindingKey = bindingKey;
   }
}

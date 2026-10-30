package com.briup.famerdemo.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ProductConsumer {

    // 👇 监听我们刚才在网页上建的那个队列
    @RabbitListener(queues = "product.queue")
    public void receiveMessage(Integer productId) {
        System.out.println("======================================");
        System.out.println(">>>> 收到 MQ 消息！商品ID是：" + productId);
        System.out.println(">>>> 假装我正在后台偷偷更新分类数量...");
        System.out.println("======================================");
    }
}
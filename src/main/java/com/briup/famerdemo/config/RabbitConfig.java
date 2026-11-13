package com.briup.famerdemo.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // ================= 1. 死信相关组件 =================
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange("app.dlx.exchange");
    }

    @Bean
    public Queue dlqQueue() {
        return new Queue("app.dlq");
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlqQueue()).to(dlxExchange()).with("dlx");
    }

    // ================= 2. 业务队列 =================
    @Bean
    public Queue stockQueue() {
        // 使用 QueueBuilder 挂载死信交换机
        return QueueBuilder.durable("product.stock.queue")
                .withArgument("x-dead-letter-exchange", "app.dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "dlx")
                .build();
    }

    @Bean
    public Queue productQueue() {
        // 商品队列如果不需要死信可以先不挂
        return QueueBuilder.durable("product.queue")
                .withArgument("x-dead-letter-exchange", "app.dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "dlx")
                .build();
    }
}
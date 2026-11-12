package com.briup.famerdemo.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitConfig {

    // 死信交换机 + 死信队列（所有业务队列共用一条就行，先跑通）
    @Bean public DirectExchange dlxExchange() {
        return new DirectExchange("app.dlx.exchange");
    }
    @Bean public Queue dlq() { return new Queue("app.dlq"); }
    @Bean public Binding dlqBinding() {
        return BindingBuilder.bind(dlq()).to(dlxExchange()).with("dlx");
    }

    // 业务队列：挂上死信路由 + 限制长度/过期，作为"兜底触发死信"的条件
    @Bean
    public Queue stockQueue() {
        return QueueBuilder.durable("product.stock.queue")
                .withArgument("x-dead-letter-exchange", "app.dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "dlx")
                .withArgument("x-max-length", 100_000)      // 满了挤到死信
                .build();
    }

    @Bean
    public Queue productQueue() {
        return QueueBuilder.durable("product.queue")
                .withArgument("x-dead-letter-exchange", "app.dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "dlx")
                .build();
    }
}

package com.briup.famerdemo.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitConfig {
    @Bean
    public Queue stockQueue(){
        return new Queue("product.stock.queue");
    }
    @Bean
    public Queue productQueue() {
        return new Queue("product.queue");
    }
}

package com.briup.famerdemo.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DeadLetterConsumer {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = "app.dlq")
    public void onDeadLetter(Message msg) {
        String body = new String(msg.getBody());
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent("dlq:lock:" + body, "1", 10, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(first)) {
            log.warn("【死信】重复投递，跳过 body={}", body);
            return;
        }

        Object raw = msg.getMessageProperties().getHeaders().get("x-first-death-queue");
        String originQueue = raw == null ? "unknown"
                : (raw instanceof byte[] b ? new String(b) : raw.toString());

        log.error("【死信】来源队列={}, body={}", originQueue, body);

        // 按来源分流处理
        if ("product.stock.queue".equals(originQueue)) {
            // stock 死信：DB 扣减没成功，把 Redis 预扣加回去
            String[] parts = body.split(":");
            Integer productId = Integer.parseInt(parts[0]);
            Integer quantity = Integer.parseInt(parts[1]);
            Long after = stringRedisTemplate.opsForValue()
                    .increment("stock:" + productId, quantity);
            log.warn("【死信补偿】Redis 库存已回滚，productId={}, +{}, 现库存={}",
                    productId, quantity, after);
        } else {
            // product 死信：幂等，记录即可，可人工重放
            log.warn("【死信】product 消息消费失败，body={}，可直接重放到 product.queue 修复", body);
        }
    }

}
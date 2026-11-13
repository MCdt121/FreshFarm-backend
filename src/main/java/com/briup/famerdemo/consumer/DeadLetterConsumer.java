package com.briup.famerdemo.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeadLetterConsumer {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = "app.dlq")
    public void onDeadLetter(Message msg) {
        String body = new String(msg.getBody());

        // 兼容 byte[] 解析队列名
        Object raw = msg.getMessageProperties().getHeaders().get("x-first-death-queue");
        String originQueue = raw == null ? "unknown"
                : (raw instanceof byte[] b ? new String(b) : raw.toString());

        log.error("【死信告警】来源队列={}, body={}", originQueue, body);

        // 针对库存扣减死信，回滚 Redis
        if ("product.stock.queue".equals(originQueue)) {
            try {
                String[] parts = body.split(":");
                Integer productId = Integer.parseInt(parts[0]);
                Integer quantity = Integer.parseInt(parts[1]);

                // 把预扣的库存加回去
                Long after = stringRedisTemplate.opsForValue()
                        .increment("stock:" + productId, quantity);
                log.warn("【死信补偿】Redis 库存已回滚，productId={}, +{}, 现库存={}",
                        productId, quantity, after);
            } catch (Exception e) {
                log.error("【死信补偿】回滚 Redis 失败，需人工介入", e);
            }
        } else {
            log.warn("【死信】其他队列消息进入死信，可人工排查，body={}", body);
        }
    }
}
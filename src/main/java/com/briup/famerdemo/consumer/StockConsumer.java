package com.briup.famerdemo.consumer;

import com.briup.famerdemo.mapper.ProductMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class StockConsumer {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = "product.stock.queue")
    public void handleStockDeduct(String message) {
        String[] parts = message.split(":");
        if (parts.length != 3) {
            log.error("库存消息格式非法，message={}", message);
            throw new AmqpRejectAndDontRequeueException("库存消息格式非法");
        }

        String messageId = parts[0];
        Integer productId = Integer.parseInt(parts[1]);
        Integer quantity = Integer.parseInt(parts[2]);

        // 消费幂等：同一条消息只处理一次
        String idempotentKey = "mq:stock:consume:" + messageId;
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(first)) {
            log.info("重复库存消息，跳过，messageId={}", messageId);
            return;
        }

        try {
            int rows = productMapper.deductStock(productId, quantity);
            if (rows > 0) {
                log.info(">>> 数据库库存扣减成功！商品ID:{}，扣减数量:{}", productId, quantity);
            } else {
                log.error(">>> 数据库库存不足，扣减失败！商品ID:{}，准备进入死信", productId);
                // 业务失败，删除幂等键，允许重试/死信补偿逻辑处理
                stringRedisTemplate.delete(idempotentKey);
                throw new AmqpRejectAndDontRequeueException("DB库存不足:" + productId);
            }
        } catch (AmqpRejectAndDontRequeueException e) {
            throw e;
        } catch (Exception e) {
            log.error("库存扣减异常，messageId={}, productId={}", messageId, productId, e);
            stringRedisTemplate.delete(idempotentKey);
            throw new AmqpRejectAndDontRequeueException("库存扣减异常:" + productId);
        }
    }
}
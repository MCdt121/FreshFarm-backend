package com.briup.famerdemo.consumer;

import com.briup.famerdemo.mapper.ProductMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StockConsumer {

    @Resource
    private ProductMapper productMapper;

    @RabbitListener(queues = "product.stock.queue")
    public void handleStockDeduct(String message) {
        String[] parts = message.split(":");
        Integer productId = Integer.parseInt(parts[0]);
        Integer quantity = Integer.parseInt(parts[1]);

        // 这里调用你写的带条件的 SQL
        int rows = productMapper.deductStock(productId, quantity);

        if (rows > 0) {
            log.info(">>> 数据库库存扣减成功！商品ID:{}，扣减数量:{}", productId, quantity);
        } else {
            log.error(">>> 数据库库存不足，扣减失败！商品ID:{}", productId);
        }
    }
}
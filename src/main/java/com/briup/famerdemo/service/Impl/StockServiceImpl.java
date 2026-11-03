package com.briup.famerdemo.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.briup.famerdemo.common.BizException;
import com.briup.famerdemo.entity.Product;
import com.briup.famerdemo.mapper.ProductMapper;
import com.briup.famerdemo.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StockServiceImpl extends ServiceImpl<ProductMapper, Product> implements StockService {
    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void deduct(Integer productId,Integer quantity){
        String stockKey = "stock:" + productId;
        //预热：如果 Redis 里没有，先从数据库读出来放进去
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(stockKey))) {
            Product product = productMapper.selectById(productId);
            if (product != null) {
                stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(product.getStock()));
            } else {
                throw new BizException("商品不存在");
            }
        }

        // 扣减 Redis 库存
        Long currentStock = stringRedisTemplate.opsForValue().decrement(stockKey, quantity);

        // 如果扣完发现小于 0，说明库存不够，再把扣的加回去，并抛出异常
        if (currentStock == null || currentStock < 0) {
            stringRedisTemplate.opsForValue().increment(stockKey, quantity); // 回滚
            throw new BizException("库存不足，抢购失败！");
        }

        // Redis 扣减成功，发 MQ 消息，异步去扣数据库
        String mqMessage = productId + ":" + quantity;
        try {
            rabbitTemplate.convertAndSend("", "product.stock.queue", mqMessage);
        } catch (Exception e) {
            // 消息没发出去，Redis 的扣减必须退回去，否则 Redis 和 DB 永久失衡
            stringRedisTemplate.opsForValue().increment(stockKey, quantity);
            log.error("MQ 发送失败，已回滚 Redis 库存", e);
            throw new BizException("下单失败，请重试");
        }
        log.info("Redis 预扣减成功！已发送 MQ，商品ID:{}，扣减数量:{}", productId, quantity);
    }
}

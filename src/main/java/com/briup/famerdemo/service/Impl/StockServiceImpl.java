package com.briup.famerdemo.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.briup.famerdemo.common.BizException;
import com.briup.famerdemo.entity.Product;
import com.briup.famerdemo.mapper.ProductMapper;
import com.briup.famerdemo.service.StockService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
public class StockServiceImpl extends ServiceImpl<ProductMapper, Product> implements StockService {
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private DefaultRedisScript<Long> stockScript;

    @PostConstruct
    public void init() {
        // 加载 Lua 脚本
        stockScript = new DefaultRedisScript<>();
        stockScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("stock.lua")));
        stockScript.setResultType(Long.class);
    }

    @Override
    public void deduct(Integer productId, Integer quantity) {
        String stockKey = "stock:" + productId;

        // 1. 预热：如果 Redis 里没有，先从数据库读出来放进去 (和你之前一样)
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(stockKey))) {
            Product product = productMapper.selectById(productId);
            if (product != null) {
                stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(product.getStock()));
            } else {
                throw new BizException("商品不存在");
            }
        }

        try {
            // 2. 执行 Lua 脚本：无锁化、单线程原子扣减
            Long result = stringRedisTemplate.execute(
                    stockScript,
                    Collections.singletonList(stockKey),
                    String.valueOf(quantity)
            );

            // 3. 判断结果
            if (result == null || result == 0) {
                throw new BizException("库存不足，抢购失败！");
            } else if (result == -1) {
                throw new BizException("商品数据异常，请稍后再试");
            }

            // 4. 扣减成功，发 MQ 消息，异步去扣数据库
            String mqMessage = productId + ":" + quantity;
            try {
                rabbitTemplate.convertAndSend("", "product.stock.queue", mqMessage);
            } catch (Exception e) {
                // 消息没发出去，必须把 Lua 扣的库存加回去 (回滚)
                stringRedisTemplate.opsForValue().increment(stockKey, quantity);
                log.error("MQ 发送失败，已回滚 Redis 库存", e);
                throw new BizException("下单失败，请重试");
            }
            log.info("Redis Lua 预扣减成功！已发送 MQ，商品ID:{}，扣减数量:{}", productId, quantity);

        } catch(BizException e){
            throw  e;
        } catch (Exception e) {
            log.error("扣减库存发生异常，商品ID:{}", productId, e);
            throw new BizException("当前抢购人数过多，服务繁忙");
        }
    }
}
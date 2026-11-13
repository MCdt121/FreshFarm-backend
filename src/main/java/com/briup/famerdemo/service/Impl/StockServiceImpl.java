package com.briup.famerdemo.service.Impl;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
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
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final AtomicBoolean redisAvailable = new AtomicBoolean(true);
    private volatile long lastRecoverCheck = 0L;
    private static final long RECOVER_CHECK_INTERVAL_MS = 5_000;

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
        if (!redisAvailable.get()) {
            deductByDb(productId, quantity);
            return;
        }

        try {
            // 预热：如果 Redis 里没有，先从数据库读出来放进去 (和你之前一样)
            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(stockKey))) {
                Product product = productMapper.selectById(productId);
                if (product != null) {
                    stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(product.getStock()));
                } else {
                    throw new BizException("商品不存在");
                }
            }
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
            String messageId = java.util.UUID.randomUUID().toString();
            String mqMessage = messageId + ":" + productId + ":" + quantity;
            try {
                rabbitTemplate.convertAndSend("", "product.stock.queue", mqMessage);
            } catch (Exception e) {
                // 消息没发出去，必须把 Lua 扣的库存加回去 (回滚)
                stringRedisTemplate.opsForValue().increment(stockKey, quantity);
                log.error("MQ 发送失败，已回滚 Redis 库存", e);
                throw new BizException("下单失败，请重试");
            }
            log.info("Redis Lua 预扣减成功！已发送 MQ，商品ID:{}，扣减数量:{}", productId, quantity);

        } catch (BizException e) {
            throw e;                          // 库存不足等业务异常，原样抛，不降级
        } catch (Exception e) {
            if (isRedisDown(e)) {
                log.warn("Redis 不可用，切换到 DB 降级，productId={}, cause={}",
                        productId, e.getClass().getSimpleName());
                redisAvailable.set(false);
                deductByDb(productId, quantity);   // 本次直接降级
            } else {
                log.error("扣减库存发生异常，商品ID:{}", productId, e);
                throw new BizException("下单失败，请重试");
            }
        }
    }
    @Override
    public void deductByDb(Integer productId, Integer quantity) {
        tryRecoverRedis();

        Entry entry;
        try {
            entry = SphU.entry("stockDeductDbFallback");
        } catch (BlockException be) {
            throw new BizException("当前访问人数过多，请稍后再试");
        }

        try {
            int rows = productMapper.deductStock(productId, quantity);
            if (rows == 0) {
                throw new BizException("库存不足，抢购失败！");
            }
            log.info("DB 降级扣减成功，productId={}, quantity={}", productId, quantity);
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    private void tryRecoverRedis() {
        if (redisAvailable.get()) return;
        long now = System.currentTimeMillis();
        if (now - lastRecoverCheck < RECOVER_CHECK_INTERVAL_MS) return;
        lastRecoverCheck = now;
        try {
            stringRedisTemplate.hasKey("__recover_probe__");
            redisAvailable.set(true);
            log.info("Redis 已恢复，切回 Redis 扣减链路");
        } catch (Exception ignored) {
        }
    }

    private boolean isRedisDown(Throwable e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof RedisConnectionFailureException
                    || t instanceof QueryTimeoutException
                    || t instanceof RedisSystemException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
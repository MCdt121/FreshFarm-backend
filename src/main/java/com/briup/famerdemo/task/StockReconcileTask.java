package com.briup.famerdemo.task;

import com.briup.famerdemo.entity.Product;
import com.briup.famerdemo.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class StockReconcileTask {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ProductMapper productMapper;

    @Scheduled(fixedDelay = 300_000)
    public void reconcileStock() {
        List<Product> products = productMapper.selectList(null);
        if (products == null || products.isEmpty()) return;

        for (Product p : products) {
            String key = "stock:" + p.getId();
            try {
                String redisVal = stringRedisTemplate.opsForValue().get(key);
                if (redisVal == null) continue;

                int redisStock;
                try {
                    redisStock = Integer.parseInt(redisVal);
                } catch (NumberFormatException nfe) {
                    log.error("【对账】Redis 值非法 productId={}, redis={}", p.getId(), redisVal);
                    continue;
                }

                if (redisStock != p.getStock()) {
                    // 只告警，不直接覆盖：在途扣减可能尚未落库，直接 set 会冲掉
                    log.error("【对账】库存不一致 productId={}, redis={}, db={}，需人工或加锁后修复",
                            p.getId(), redisStock, p.getStock());
                }
            } catch (Exception e) {
                log.error("【对账】处理 productId={} 失败", p.getId(), e);
            }
        }
    }
}
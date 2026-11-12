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
                if (redisVal == null) continue; // 没预热过的商品，不参与对账

                int redisStock;
                try {
                    redisStock = Integer.parseInt(redisVal);
                } catch (NumberFormatException nfe) {
                    log.error("【对账】Redis 值非法 productId={}, redis={}", p.getId(), redisVal);
                    redisStock = -1;
                }

                if (redisStock != p.getStock()) {
                    log.error("【对账】库存不一致 productId={}, redis={}, db={}",
                            p.getId(), redisVal, p.getStock());
                    stringRedisTemplate.opsForValue()
                            .set(key, String.valueOf(p.getStock()));
                }
            } catch (Exception e) {
                log.error("【对账】处理 productId={} 失败", p.getId(), e);
            }
        }
    }
}
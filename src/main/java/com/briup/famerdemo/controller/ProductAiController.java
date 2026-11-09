package com.briup.famerdemo.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.briup.famerdemo.common.Result;
import com.briup.famerdemo.entity.Product;
import com.briup.famerdemo.mapper.ProductMapper;
import com.briup.famerdemo.service.AiDescService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@Slf4j
@SaCheckLogin
@RestController
@RequestMapping("/product")
public class ProductAiController {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private AiDescService aiDescService;
    @Autowired
    private ProductMapper productMapper;

    @PostMapping("/ai-desc/{id}")
    @SentinelResource(value = "aiDesc", blockHandler = "aiDescBlock")
    public Result<String> aiDesc(@PathVariable Integer id) {
        String cacheKey = "ai:desc:" + id;

        // 缓存
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("AI 文案命中缓存，productId={}", id);
            return Result.success(cached);
        }

        // 数据来自数据库
        Product product = productMapper.selectById(id);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }

        // 调用 + 降级 + 校验
        String desc;
        try {
            desc = aiDescService.generateDesc(product);
            if (desc == null || desc.isBlank() || desc.length() > 200) {
                throw new IllegalStateException("模型输出不合法");
            }
        } catch (Exception e) {
            log.warn("AI 文案生成失败，降级为模板文案，productId={}", id, e);
            desc = product.getName() + "，产地直供，新鲜可溯";
        }

        // 写回缓存
        // 注意：spring-data-redis 4.x 的 set(key,value,ttl,unit) 存在递归缺陷，
        // 这里拆成 set + expire，非原子；生产应改用 setEx 或修版本
        stringRedisTemplate.opsForValue().set(cacheKey, desc);
        stringRedisTemplate.expire(cacheKey, 1, TimeUnit.HOURS);
        return Result.success(desc);
    }

    // 被限流时走这里
    public Result<String> aiDescBlock(Integer id, BlockException e) {
        return Result.error(429, "生成请求太频繁，请稍后再试");
    }
}

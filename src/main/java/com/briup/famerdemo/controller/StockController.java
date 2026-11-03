package com.briup.famerdemo.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.briup.famerdemo.common.Result;
import com.briup.famerdemo.entity.DeductRequest;
import com.briup.famerdemo.entity.Product;
import com.briup.famerdemo.mapper.ProductMapper;
import com.briup.famerdemo.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/stock")
public class StockController {
    @Autowired
    private StockService stockService;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/deduct")
    public Result deduct(@Validated @RequestBody DeductRequest request){
        stockService.deduct(request.getProductId(), request.getQuantity());
        return Result.success("扣减成功");
    }
    @PostMapping("/sync/{productId}")
    public Result sync(@PathVariable Integer productId) {
        Product p = productMapper.selectById(productId);
        if (p == null) return Result.error(404, "商品不存在");
        stringRedisTemplate.opsForValue().set("stock:" + productId, String.valueOf(p.getStock()));
        return Result.success("已从数据库同步：" + p.getStock());
    }
}

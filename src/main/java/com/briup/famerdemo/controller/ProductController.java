package com.briup.famerdemo.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.briup.famerdemo.common.Result;
import com.briup.famerdemo.entity.Product;
import com.briup.famerdemo.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@SaCheckLogin
@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private ProductService productService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostMapping("/add")
    public Result<Void> addProduct(@Validated @RequestBody Product product){
        productService.save(product);
        // 发消息给分类ID
        rabbitTemplate.convertAndSend("", "product.queue", product.getCategoryId());
        log.info("商品保存成功，MQ消息发送完毕！");
        return Result.success(null);
    }

    @SentinelResource(value = "getProduct", blockHandler = "handleGetBlock", fallback = "handleGetFallback")
    @GetMapping("/get/{id}")
    public Result<Product> getProductById(@PathVariable Integer id){
        return Result.success(productService.getById(id));
    }
    // 被限流 / 熔断时
    public Result<Product> handleBlock(Integer id, BlockException e) {
        log.warn("getProduct 被限流或熔断，id={}", id);
        return Result.error(429, "当前访问人数较多，请稍后再试");
    }

    // 方法内部异常时
    public Result<Product> handleFallback(Integer id, Throwable t) {
        log.error("getProduct 执行失败，id={}", id, t);
        return Result.error(503, "服务暂时不可用，请稍后再试");
    }

    @PostMapping("/update")
    public Result<Void> updateProduct(@Validated @RequestBody Product product){
        boolean ok = productService.updateById(product);
        return ok ? Result.success(null) : Result.error(404, "商品更新失败");
    }

    @DeleteMapping("/remove/{id}")
    public Result<Void> removeProduct(@PathVariable Integer id){
        productService.removeById(id);
        return Result.success(null);
    }
}
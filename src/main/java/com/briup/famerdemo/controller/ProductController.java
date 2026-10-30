package com.briup.famerdemo.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
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

    @GetMapping("/get/{id}")
    public Result<Product> getProductById(@PathVariable Integer id){
        return Result.success(productService.getById(id));
    }

    @PostMapping("/update")
    public Result<Void> updateProduct(@Validated @RequestBody Product product){
        productService.updateById(product);
        return Result.success(null);
    }

    @DeleteMapping("/remove/{id}")
    public Result<Void> removeProduct(@PathVariable Integer id){
        productService.removeById(id);
        return Result.success(null);
    }
}
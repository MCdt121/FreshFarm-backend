package com.briup.famerdemo.controller;


import cn.dev33.satoken.annotation.SaCheckLogin;
import com.briup.famerdemo.entity.Product;
import com.briup.famerdemo.service.ProductService;
import org.apache.ibatis.annotations.Delete;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@SaCheckLogin
@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private ProductService productService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostMapping("/add")
    public String addProduct(@RequestBody Product product){
        productService.save(product);
        rabbitTemplate.convertAndSend("", "product.queue", product.getId());
        System.out.println("商品保存成功，MQ消息发送完毕！");
        return "新增成功";
    }
    @GetMapping("/get/{id}")
    public Product getProductById(@PathVariable Integer id){
        return productService.getById(id);
    }
    @PostMapping("/update")
    public String updateProduct(@RequestBody Product product){
        productService.updateById(product);
        return "更改成功";
    }
    @DeleteMapping("/remove/{id}")
    public String removeProduct(@PathVariable Integer id){
        productService.removeById(id);
        return "删除成功";
    }
}

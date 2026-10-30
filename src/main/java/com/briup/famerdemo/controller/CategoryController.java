package com.briup.famerdemo.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.briup.famerdemo.entity.Category;
import com.briup.famerdemo.entity.Product;
import com.briup.famerdemo.service.CategoryService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@SaCheckLogin
@RestController
@RequestMapping("/kind")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @PostMapping("/add")
    public String addCategory(@RequestBody Category category){
        categoryService.save(category);
        rabbitTemplate.convertAndSend("", "product.queue", category.getId());
        System.out.println("商品保存成功，MQ消息发送完毕！");
        return "新增成功";
    }
    @GetMapping("/get/{id}")
    public Category getCategoryId(@PathVariable Integer id){
        return categoryService.getById(id);
    }
    @PostMapping("/update")
    public String updateCategory(@RequestBody Category category){
        categoryService.updateById(category);
        return "更改成功";
    }
    @DeleteMapping("/remove/{id}")
    public String removeCategory(@PathVariable Integer id){
        categoryService.removeById(id);
        return "删除成功";
    }
}

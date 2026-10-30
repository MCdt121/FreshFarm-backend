package com.briup.famerdemo.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.briup.famerdemo.common.Result;
import com.briup.famerdemo.entity.Category;
import com.briup.famerdemo.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@SaCheckLogin
@RestController
@RequestMapping("/kind")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @PostMapping("/add")
    public Result<Void> addCategory(@RequestBody Category category){
        categoryService.save(category);
        log.info("分类保存成功！");
        return Result.success(null);
    }

    @GetMapping("/get/{id}")
    public Result<Category> getCategoryId(@PathVariable Integer id){
        return Result.success(categoryService.getById(id));
    }

    @PostMapping("/update")
    public Result<Void> updateCategory(@RequestBody Category category){
        categoryService.updateById(category);
        return Result.success(null);
    }

    @DeleteMapping("/remove/{id}")
    public Result<Void> removeCategory(@PathVariable Integer id){
        categoryService.removeById(id);
        return Result.success(null);
    }
}
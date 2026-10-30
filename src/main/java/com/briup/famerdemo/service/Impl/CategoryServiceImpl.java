package com.briup.famerdemo.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.briup.famerdemo.common.BizException;
import com.briup.famerdemo.entity.Category;
import com.briup.famerdemo.entity.Product;
import com.briup.famerdemo.mapper.CategoryMapper;
import com.briup.famerdemo.mapper.ProductMapper;
import com.briup.famerdemo.service.CategoryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    @Resource
    private ProductMapper productMapper;
    @Override
    public boolean removeById(Serializable id) {
        //  统计这个分类下有多少个商品
        Long count = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, id)
        );

        // 如果大于0，业务异常，被全局异常处理器拦截
        if (count > 0) {
            throw new BizException("该分类下还有 " + count + " 个商品，无法删除！");
        }

        // 如果没有商品，放行删除
        return super.removeById(id);
    }
}


package com.briup.famerdemo.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.briup.famerdemo.common.BizException;
import com.briup.famerdemo.entity.Product;
import com.briup.famerdemo.mapper.ProductMapper;
import com.briup.famerdemo.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockServiceImpl extends ServiceImpl<ProductMapper, Product> implements StockService {
    @Autowired
    private ProductMapper productMapper;

    @Override
    public void deduct(Integer productId,Integer quantity){
        int rows = productMapper.deductStock(productId,quantity);
        if (rows==0){
            throw new BizException("库存不足");
        }
    }
}

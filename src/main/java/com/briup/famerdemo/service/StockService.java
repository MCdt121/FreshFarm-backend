package com.briup.famerdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.briup.famerdemo.entity.Product;

public interface StockService extends IService<Product> {
    void deduct(Integer productId, Integer quantity);
    void deductByDb(Integer productId, Integer quantity);
}

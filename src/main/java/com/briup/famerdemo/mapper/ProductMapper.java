package com.briup.famerdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.briup.famerdemo.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}

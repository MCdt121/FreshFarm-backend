package com.briup.famerdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.briup.famerdemo.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    @Update("UPDATE product SET stock=stock- #{quantity} WHERE id = #{productId} AND stock>=#{quantity}")
    int deductStock(@Param("productId") Integer productId,@Param("quantity") Integer quantity);
}

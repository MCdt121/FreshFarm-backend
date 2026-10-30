package com.briup.famerdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.briup.famerdemo.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}

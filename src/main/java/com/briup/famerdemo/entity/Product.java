package com.briup.famerdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("product")
public class Product {
    @TableId(value="id",type= IdType.AUTO)
    private Integer id;
    private int category_id;
    private String name;
    private double price;

}

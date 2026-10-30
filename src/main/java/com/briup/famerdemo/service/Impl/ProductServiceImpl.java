package com.briup.famerdemo.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.briup.famerdemo.entity.Product;
import com.briup.famerdemo.mapper.ProductMapper;
import com.briup.famerdemo.service.ProductService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService  {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Product getById(java.io.Serializable id){
        // 查缓存：根据 ID 拼出key，然后去Redis get；
        String key="product:"+id;
        //try catch防止Redis挂掉，挂掉后直接查询数据库
        try{
            //去Redis查找这个key，返回到cacheValue
            String cacheValue=stringRedisTemplate.opsForValue().get(key);

            //防止缓存穿透：如果不是null而是空字符，说明有人查过这个ID，直接防止查询
            if(cacheValue!=null){
                if(cacheValue.isEmpty()){
                    System.out.println("命中空值缓存，防止穿透");
                    return null;
                }
                Product product = JSON.parseObject(cacheValue, Product.class);
                System.out.println("命中缓存，直接返回");
                //命中缓存：查出来不是NULL也不是空字符串，把字符串传回前端
                return product;
            }
        }catch (Exception e){
            System.out.println("Redis 连接失败，查询数据库");
        }
        //程序走到这说明缓存里什么都没有，要去数据库里找
        Product product=this.getBaseMapper().selectById(id);
        //数据库里也查不到
        if (product == null) {
            try {
                // 防止恶意查询，往Redis里放空盒子，设置一分钟过期
                stringRedisTemplate.opsForValue().set(key, "", 1, TimeUnit.MINUTES);
            } catch (Exception ignored) {}
            System.out.println("数据库也没查到，存入空值缓存！");
            return null;
        }

        // 数据库有，存入 Redis
        try {
            stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(product), 30, TimeUnit.MINUTES);
        } catch (Exception ignored) {}

        System.out.println("数据库查到，写入缓存！");
        //把商品交给前端
        return product;
    }
    @Override
    public boolean updateById(Product product) {
        // 1. 先更新数据库
        boolean success = super.updateById(product);

        // 2. 如果更新成功，立刻去把 Redis 里的旧数据删掉！
        if (success) {
            String key = "product:" + product.getId();
            try {
                stringRedisTemplate.delete(key);
                System.out.println("商品修改成功，缓存已删除！");
            } catch (Exception e) {
                System.out.println("缓存删除失败，但不影响主流程");
            }
        }
        return success;
    }
    @Override
    public boolean removeById(java.io.Serializable id) {
        // 1. 先删除数据库记录
        boolean success = super.removeById(id);

        // 2. 删库成功，立刻删缓存
        if (success) {
            String key = "product:" + id;
            try {
                stringRedisTemplate.delete(key);
                System.out.println("商品删除成功，缓存已删除！");
            } catch (Exception e) {
                System.out.println("缓存删除失败");
            }
        }
        return success;
    }
}

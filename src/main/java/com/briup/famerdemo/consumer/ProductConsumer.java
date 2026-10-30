package com.briup.famerdemo.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.briup.famerdemo.entity.Category;
import com.briup.famerdemo.entity.Product;
import com.briup.famerdemo.mapper.CategoryMapper;
import com.briup.famerdemo.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;

@Slf4j
@Component
public class ProductConsumer {

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private ProductMapper productMapper;

    @RabbitListener(queues = "product.queue")
    public void receiveMessage(Integer categoryId) {
        log.info(">>>> 收到 MQ 消息！准备异步更新分类统计，分类ID：" + categoryId);

        if (categoryId != null) {
            Long actualCount = productMapper.selectCount(
                    new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, categoryId)
            );

            // 幂等:把算出来的真实数量，覆盖更新到分类表里
            UpdateWrapper<Category> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", categoryId).set("product_count", actualCount);
            categoryMapper.update(null, updateWrapper);

            log.info(">>>> 分类统计更新成功！最新数量为：" + actualCount);
        }
    }
}
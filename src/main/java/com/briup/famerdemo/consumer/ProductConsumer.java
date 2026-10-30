package com.briup.famerdemo.consumer;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.briup.famerdemo.entity.Category;
import com.briup.famerdemo.mapper.CategoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;

@Slf4j
@Component
public class ProductConsumer {

    @Resource
    private CategoryMapper categoryMapper;

    @RabbitListener(queues = "product.queue")
    public void receiveMessage(Integer categoryId) {
        log.info(">>>> 收到 MQ 消息！准备异步更新分类统计，分类ID：" + categoryId);

        if (categoryId != null) {
            // 真正去更新数据库：商品数量 + 1
            UpdateWrapper<Category> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", categoryId).setSql("product_count = product_count + 1");
            categoryMapper.update(null, updateWrapper);

            log.info(">>>> 分类统计更新成功！");
        }
    }
}
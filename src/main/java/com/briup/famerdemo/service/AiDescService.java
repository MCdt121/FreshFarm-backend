package com.briup.famerdemo.service;

import com.briup.famerdemo.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class AiDescService {
    private final DeepSeekChatModel chatModel;

    public String generateDesc(Product product){
        String prompt = """
                你是电商文案。根据下面商品信息，写一段 40 字以内的卖点文案。
                只输出文案本身，不要解释、不要加引号、不要换行。
                商品名：%s
                分类ID：%s
                价格：%s 元
                """.formatted(product.getName(), product.getCategoryId(), product.getPrice());

        String result = chatModel.call(prompt);
        log.info("AI 生成文案成功，productId={}，长度={}", product.getId(), result.length());
        return result;
    }


}



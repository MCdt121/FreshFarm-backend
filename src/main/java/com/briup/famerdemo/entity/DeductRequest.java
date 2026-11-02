package com.briup.famerdemo.entity; // 如果你没有dto包，放entity包下也行

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeductRequest {
    @NotNull(message = "商品ID不能为空")
    private Integer productId;

    @NotNull(message = "扣减数量不能为空")
    @Min(value = 1, message = "扣减数量至少为1")
    private Integer quantity;
}
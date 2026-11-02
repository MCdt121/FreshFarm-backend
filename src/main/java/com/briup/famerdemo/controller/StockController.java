package com.briup.famerdemo.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.briup.famerdemo.common.Result;
import com.briup.famerdemo.entity.DeductRequest;
import com.briup.famerdemo.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/stock")
public class StockController {
    @Autowired
    private StockService stockService;

    @PostMapping("/deduct")
    public Result deduct(@Validated @RequestBody DeductRequest request){
        stockService.deduct(request.getProductId(), request.getQuantity());
        return Result.success("扣减成功");
    }
}

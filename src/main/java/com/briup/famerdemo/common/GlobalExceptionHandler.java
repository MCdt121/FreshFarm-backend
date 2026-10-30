package com.briup.famerdemo.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public Result<String> handleBizException(BizException e){
        System.out.println("业务异常被拦截:"+e.getMessage());
        return Result.error(e.getMessage());
    }
    @ExceptionHandler(Exception.class)
    public Result<String> handleBizException(Exception e){
        e.printStackTrace();
        return Result.error("系统繁忙，稍后重试");
    }
    @ExceptionHandler(cn.dev33.satoken.exception.NotLoginException.class)
    public Result<String> handleNotLoginException(cn.dev33.satoken.exception.NotLoginException e) {
        System.out.println("【未登录拦截】：" + e.getMessage());
        // 返回 401 状态码，提示前端去登录
        return Result.error(401,"未登录，请先登录！");
    }


}

package com.briup.famerdemo.common;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public Result<String> handleBizException(BizException e){
        System.out.println("业务异常被拦截:"+e.getMessage());
        return Result.error(400,e.getMessage());
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
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValidException(MethodArgumentNotValidException e) {
        // 获取第一个校验失败的提示信息
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        System.out.println("【参数校验拦截】：" + message);
        return Result.error(400, message); // 返回 400 表示参数错误
    }
    @ExceptionHandler(com.alibaba.csp.sentinel.slots.block.BlockException.class)
    public Result<String> handleBlockException(BlockException e) {
        System.out.println("【Sentinel 限流/熔断】：" + e.getRule());
        return Result.error(429, "当前访问人数较多，请稍后再试");
    }

}

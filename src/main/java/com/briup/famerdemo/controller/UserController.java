package com.briup.famerdemo.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.briup.famerdemo.common.BizException;
import com.briup.famerdemo.common.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    @PostMapping("/login")
    public Result<String> login(@RequestParam String username, @RequestParam String password) {
        // 用户:账号 admin，密码 123456
        if ("admin".equals(username) && "123456".equals(password)) {
            // 核心：调用 Sa-Token 登录，参数是用户 ID
            StpUtil.login(10001);

            // 获取生成的 Token，返回给前端
            String tokenValue = StpUtil.getTokenValue();
            return Result.success(tokenValue);
        }

        // 如果密码不对，抛出我们写好的业务异常
        throw new BizException("用户名或密码错误！");
    }

    // 测试获取用户信息的接口
    @GetMapping("/info")
    public Result<String> info() {
        return Result.success("当前登录的用户ID是：" + StpUtil.getLoginId());
    }
}
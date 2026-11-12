package com.briup.famerdemo.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SentinelConfig {
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    @PostConstruct
    public void initRules() {
        //stock商品扣减降级限流
        FlowRule dbFallback = new FlowRule();
        dbFallback.setResource("stockDeductDbFallback");
        dbFallback.setGrade(RuleConstant.FLOW_GRADE_QPS);
        dbFallback.setCount(10);
        dbFallback.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER);
        dbFallback.setMaxQueueingTimeMs(500);
        //
        FlowRule gp = new FlowRule();
        gp.setResource("getProduct");
        gp.setGrade(RuleConstant.FLOW_GRADE_QPS);
        gp.setCount(5);
        //springai
        FlowRule ai = new FlowRule();
        ai.setResource("aiDesc");
        ai.setGrade(RuleConstant.FLOW_GRADE_QPS);
        ai.setCount(5);

        FlowRule stock = new FlowRule();
        stock.setResource("stockDeduct");
        stock.setGrade(RuleConstant.FLOW_GRADE_QPS);
        stock.setCount(10);   // 按你压测情况调整
        FlowRuleManager.loadRules(List.of(gp, ai, stock,dbFallback));

        DegradeRule dg = new DegradeRule("getProduct");
        dg.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        dg.setCount(0.5);
        dg.setTimeWindow(10);
        dg.setMinRequestAmount(5);

        // 如果库存接口也要异常熔断，注意配置 exceptionsToIgnore = BizException.class
        // 否则“库存不足”会把 stockDeduct 熔断
        DegradeRule stockDg = new DegradeRule("stockDeduct");
        stockDg.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        stockDg.setCount(0.5);
        stockDg.setTimeWindow(10);
        stockDg.setMinRequestAmount(20);
        DegradeRuleManager.loadRules(List.of(dg, stockDg));


    }
}

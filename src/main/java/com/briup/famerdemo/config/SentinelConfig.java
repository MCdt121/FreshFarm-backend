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
public void initFlowRules(){
    // 限流：这个资源 QPS 超过 5 就直接拒绝
    FlowRule flow = new FlowRule("getProduct");
    flow.setCount(5);
    flow.setGrade(RuleConstant.FLOW_GRADE_QPS);
    FlowRuleManager.loadRules(List.of(flow));

// 熔断：10 秒内异常比例超过 50% 且请求数 >5，就熔断 10 秒
    DegradeRule degrade = new DegradeRule("getProduct");
    degrade.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
    degrade.setCount(0.5);
    degrade.setTimeWindow(10);
    degrade.setMinRequestAmount(5);
    DegradeRuleManager.loadRules(List.of(degrade));
}
}

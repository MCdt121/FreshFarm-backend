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
        // 只保留降级路径的流控（保护 DB）
        FlowRule dbFallback = new FlowRule();
        dbFallback.setResource("stockDeductDbFallback");
        dbFallback.setGrade(RuleConstant.FLOW_GRADE_QPS);
        dbFallback.setCount(10); // DB 只能承受 10 QPS，保护它
        dbFallback.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER);
        dbFallback.setMaxQueueingTimeMs(500);

        // 保留商品查询和AI的规则
        FlowRule gp = new FlowRule();
        gp.setResource("getProduct");
        gp.setGrade(RuleConstant.FLOW_GRADE_QPS);
        gp.setCount(500);

        FlowRule ai = new FlowRule();
        ai.setResource("aiDesc");
        ai.setGrade(RuleConstant.FLOW_GRADE_QPS);
        ai.setCount(5);

        FlowRuleManager.loadRules(List.of(gp, ai, dbFallback));

        DegradeRule dg = new DegradeRule("getProduct");
        dg.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        dg.setCount(0.5);
        dg.setTimeWindow(10);
        dg.setMinRequestAmount(5);
        DegradeRuleManager.loadRules(List.of(dg));
    }
}

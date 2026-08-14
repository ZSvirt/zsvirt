package org.zstack.autoscaling.group.rule;

import org.zstack.zwatch.ruleengine.Rule;
import org.zstack.zwatch.ruleengine.RuleEvaluationResult;

/**
 * Created by lining on 2018/9/22.
 */
public interface AutoScalingRuleManager {
    //lining123
    void triggerAutoScalingRule(RuleEvaluationResult res, Rule rule);
}

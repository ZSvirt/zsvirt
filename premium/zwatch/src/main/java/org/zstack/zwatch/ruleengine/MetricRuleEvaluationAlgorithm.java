package org.zstack.zwatch.ruleengine;

import org.zstack.zwatch.datatype.Datapoint;

import java.util.List;

public interface MetricRuleEvaluationAlgorithm {
    List<RuleEvaluationResult> eval(List<Datapoint> data, MetricRule rule);
}

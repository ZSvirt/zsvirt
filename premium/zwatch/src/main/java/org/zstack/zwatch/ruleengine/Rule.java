package org.zstack.zwatch.ruleengine;

import java.util.List;

public interface Rule {
    List<RuleEvaluationResult> eval();

    String getUuid();

    default RuleEvaluationResultListener getRuleStateChangeListener() {
        return null;
    }

    default boolean needEvaluate() {
        return true;
    }
}

package org.zstack.zwatch.ruleengine;

import java.util.List;

public interface RuleEvaluationResultListener {
    void stateChanged(RuleEvaluationResult res, Rule rule);

    default void problemState(RuleEvaluationResult res, Rule rule) {
    }

    default boolean needReactToProblemState() {
        return true;
    }

    default void executeActionsFromProblemResults(List<RuleEvaluationResult> results, Rule rule) {

    }

    default void executeActionsFromRecoveredResults(List<RuleEvaluationResult> results, Rule rule) {

    }

}

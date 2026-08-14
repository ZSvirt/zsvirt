package org.zstack.zwatch.ruleengine;

public class RuleEvaluationResult {
    public enum RuleEvaluationState {
        OK,
        Problem,
        NoData,
    }

    private RuleEvaluationState currentState;
    private RuleEvaluationState lastState;
    private Double currentValue;
    private String identifyLabel;
    private String context;

    public Double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Double currentValue) {
        this.currentValue = currentValue;
    }

    public RuleEvaluationState getLastState() {
        return lastState;
    }

    public void setLastState(RuleEvaluationState lastState) {
        this.lastState = lastState;
    }

    public RuleEvaluationState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(RuleEvaluationState currentState) {
        this.currentState = currentState;
    }

    public boolean isStateChanged() {
        return lastState != currentState;
    }

    public String getIdentifyLabel() {
        return identifyLabel;
    }

    public void setIdentifyLabel(String identifyLabel) {
        this.identifyLabel = identifyLabel;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}

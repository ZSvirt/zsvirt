package org.zstack.zwatch.ruleengine;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.datatype.MetricQueryObject;
import org.zstack.zwatch.datatype.Namespace;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class MetricRule implements Rule {
    private static transient CLogger logger = Utils.getLogger(MetricRule.class);

    private String uuid;
    private String namespaceName;
    private String metricName;
    private ComparisonOperator comparisonOperator;
    private List<Label> labels;
    private long duration;
    private double threshold;
    private RuleEvaluationResult.RuleEvaluationState state = RuleEvaluationResult.RuleEvaluationState.NoData;
    private transient MetricRuleEvaluationAlgorithm algorithm;
    private String accountUuid;

    public MetricRule(String uuid) {
        this.uuid = uuid;
    }

    public RuleEvaluationResult.RuleEvaluationState getState() {
        return state;
    }

    public void setState(RuleEvaluationResult.RuleEvaluationState state) {
        this.state = state;
    }

    public MetricRuleEvaluationAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(MetricRuleEvaluationAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public String getNamespaceName() {
        return namespaceName;
    }

    public void setNamespaceName(String namespaceName) {
        this.namespaceName = namespaceName;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public ComparisonOperator getComparisonOperator() {
        return comparisonOperator;
    }

    public void setComparisonOperator(ComparisonOperator comparisonOperator) {
        this.comparisonOperator = comparisonOperator;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    static class DefaultAlgorithm implements MetricRuleEvaluationAlgorithm {

        private RuleEvaluationResult.RuleEvaluationState evalCurrentState(List<Datapoint> data, MetricRule rule) {
            if (rule.getState() == RuleEvaluationResult.RuleEvaluationState.NoData) {
                if (isProblem(data, rule)) {
                    return RuleEvaluationResult.RuleEvaluationState.Problem;
                }

                return RuleEvaluationResult.RuleEvaluationState.OK;
            } else if (rule.getState() == RuleEvaluationResult.RuleEvaluationState.OK) {
                if (isProblem(data, rule)) {
                    return RuleEvaluationResult.RuleEvaluationState.Problem;
                }

                return RuleEvaluationResult.RuleEvaluationState.OK;
            } else if (rule.getState() == RuleEvaluationResult.RuleEvaluationState.Problem) {
                if (isProblem(data, rule)) {
                    return RuleEvaluationResult.RuleEvaluationState.Problem;
                }

                if (isOk(data, rule)) {
                    return RuleEvaluationResult.RuleEvaluationState.OK;
                }

                // note: for a rule in problem state, if the data points
                // contains normal values and problem values, we treat the
                // rule not a problem rule, because the data points do not
                // all satisfy with the problem condition
                return RuleEvaluationResult.RuleEvaluationState.OK;
            }

            throw new CloudRuntimeException(String.format("illegal current state: %s", rule.getState()));
        }

        @Override
        public List<RuleEvaluationResult> eval(List<Datapoint> data, MetricRule rule) {
            RuleEvaluationResult res = new RuleEvaluationResult();
            res.setCurrentState(evalCurrentState(data, rule));
            res.setLastState(rule.getState());
            res.setCurrentValue(evalLastValue(data, res.getCurrentState(), rule));
            return Arrays.asList(res);
        }

        private double evalLastValue(List<Datapoint> data, RuleEvaluationResult.RuleEvaluationState currentState, MetricRule rule) {
            if (currentState.equals(RuleEvaluationResult.RuleEvaluationState.Problem)) {
                Datapoint datapoint = data
                        .stream()
                        .filter(dp -> dp.compare(rule.comparisonOperator, rule.getThreshold()))
                        .findFirst().orElse(data.get(0));

                traceRule(() -> String.format("current rule state is %s, replace last value" +
                                        " to a problem data point:\n%s\n" +
                                        " from data points:\n%s\n",
                                RuleEvaluationResult.RuleEvaluationState.Problem,
                                JSONObjectUtil.dumpPretty(datapoint),
                                JSONObjectUtil.dumpPretty(data)),
                        rule.getUuid());

                return datapoint.getValue();
            } else if (currentState.equals(RuleEvaluationResult.RuleEvaluationState.OK)) {
                return data.get(0).getValue();
            } else {
                throw new CloudRuntimeException("Evaluation state should be Problem or OK");
            }
        }

        private boolean problemOrOk(List<Datapoint> data, ComparisonOperator op, MetricRule rule) {
            traceRule(() -> String.format("eval data points: %s",
                    JSONObjectUtil.dumpPretty(data)),
                    rule.getUuid());

            for (Datapoint dp : data) {
                if (!dp.compare(op, rule.getThreshold())) {
                    traceRule(() -> String.format("data point not satisfy[operator: %s, threshold: %s]" +
                                            " is found, data point: %s",
                                    op.toString(),
                                    rule.getThreshold(),
                                    JSONObjectUtil.dumpPretty(dp)),
                            rule.getUuid());
                    return false;
                }
            }

            return true;
        }

        private boolean isOk(List<Datapoint> data, MetricRule rule) {
            return problemOrOk(data, rule.getComparisonOperator().getReverseOperator(), rule);
        }

        private boolean isProblem(List<Datapoint> data, MetricRule rule) {
            return problemOrOk(data, rule.getComparisonOperator(), rule);
        }

        public void traceRule(Supplier<String> traceMessage, String ruleUuid) {
            if (!logger.isTraceEnabled() || traceMessage == null) {
                return;
            }

            logger.trace(String.format("trace metric rule[uuid: %s]: %s", ruleUuid, traceMessage.get()));
        }
    }

    @Override
    public List<RuleEvaluationResult> eval() {
        if (algorithm == null) {
            algorithm = new DefaultAlgorithm();
        }

        Namespace namespace = Namespace.getMetricNameSpace(namespaceName, metricName);

        long currentTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        MetricQueryObject qo = MetricQueryObject.New().metricName(metricName)
                .namespace(namespaceName)
                .startTime(currentTime - duration)
                .endTime(currentTime)
                .account(accountUuid)
                .labels(labels).build();

        RuleEvaluationResult ret;
        List<Datapoint> data = namespace.query(qo);
        if (data.isEmpty()) {
            ret = new RuleEvaluationResult();
            ret.setLastState(state);
            state = RuleEvaluationResult.RuleEvaluationState.NoData;
            ret.setCurrentState(state);
            return Arrays.asList(ret);
        }

        List<RuleEvaluationResult> results = algorithm.eval(data, this);

        evalRuleState(results);

        return results;
    }

    private void evalRuleState(List<RuleEvaluationResult> results) {
        if (results.stream().anyMatch(result -> result.getCurrentState().equals(RuleEvaluationResult.RuleEvaluationState.Problem))) {
            state = RuleEvaluationResult.RuleEvaluationState.Problem;
        } else if (results.stream().anyMatch(result -> result.getCurrentState().equals(RuleEvaluationResult.RuleEvaluationState.OK))) {
            state = RuleEvaluationResult.RuleEvaluationState.OK;
        } else {
            state = RuleEvaluationResult.RuleEvaluationState.NoData;
        }
    }

    @Override
    public String toString() {
        return JSONObjectUtil.toJsonString(this);
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof MetricRule && uuid.equals(((MetricRule) o).uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}

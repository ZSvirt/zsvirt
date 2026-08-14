package org.zstack.zwatch.datatype;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.utils.DebugUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MetricQueryObject {
    private String namespaceName;
    private Long startTime;
    private Long endTime;
    private Integer period;
    private String metricName;
    private List<Label> labels = new ArrayList<>();
    private List<ValueCondition> valueConditions = new ArrayList<>();
    private String accountUuid;
    private String aggregationOp;

    @Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
    public static class QueryObjectBuilder {
        @Autowired
        private Resolution resolution;

        private MetricQueryObject ret = new MetricQueryObject();

        public QueryObjectBuilder aggregationOp(String operation) {
            ret.aggregationOp = operation;
            return this;
        }

        public QueryObjectBuilder startTime(long v) {
            ret.startTime = v;
            return this;
        }

        public QueryObjectBuilder endTime(long v) {
            ret.endTime = v;
            return this;
        }

        public QueryObjectBuilder period(Integer v) {
            ret.period = v;
            return this;
        }

        public QueryObjectBuilder metricName(String v) {
            ret.metricName = v;
            return this;
        }

        public QueryObjectBuilder label(String tag, String value) {
            Label l = new Label(tag, value);
            ret.labels.add(l);
            return this;
        }

        public QueryObjectBuilder labels(Map<String, String> ls) {
            ls.forEach((k, v) -> ret.labels.add(new Label(k, v)));
            return this;
        }

        public QueryObjectBuilder labels(List<Label> ls) {
            ret.labels.addAll(ls);
            return this;
        }

        public QueryObjectBuilder valueConditions(List<ValueCondition> ls) {
            ret.valueConditions.addAll(ls);
            return this;
        }

        public QueryObjectBuilder namespace(String namespaceName) {
            if (StringUtils.isBlank(ret.namespaceName)) {
                ret.namespaceName = namespaceName;
            }
            return this;
        }

        public QueryObjectBuilder account(String accountUuid) {
            ret.accountUuid = accountUuid;
            return this;
        }

        public MetricQueryObject build() {
            DebugUtils.Assert(ret.startTime != null, "startTime() must be called");
            DebugUtils.Assert(ret.metricName != null, "metricName() must be called");
            DebugUtils.Assert(ret.namespaceName != null, "namespace() must be called");

            if (ret.period == null) {
                ret.period = resolution.resolution(ret.startTime, ret.endTime);
            }

            return ret;
        }
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getNamespaceName() {
        return namespaceName;
    }

    public void setNamespaceName(String namespaceName) {
        this.namespaceName = namespaceName;
    }

    public static QueryObjectBuilder New() {
        return new QueryObjectBuilder();
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }

    public List<ValueCondition> getValueConditions() {
        return valueConditions;
    }

    public void setValueConditions(List<ValueCondition> valueConditions) {
        this.valueConditions = valueConditions;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public String getAggregationOp() {
        return aggregationOp;
    }

    public void setAggregationOp(String aggregationOp) {
        this.aggregationOp = aggregationOp;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("MetricQueryObject{")
                .append("namespaceName='").append(namespaceName).append('\'')
                .append(", startTime=").append(startTime)
                .append(", endTime=").append(endTime)
                .append(", period=").append(period)
                .append(", metricName='").append(metricName).append('\'')
                .append(", labels=").append(labels)
                .append(", valueConditions=").append(valueConditions)
                .append(", accountUuid='").append(accountUuid).append('\'')
                .append(", aggregationOp='").append(aggregationOp).append('\'')
                .append('}');
        return stringBuilder.toString();
    }
}

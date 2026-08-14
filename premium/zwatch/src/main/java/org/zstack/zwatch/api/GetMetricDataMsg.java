package org.zstack.zwatch.api;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by lining on 2018/10/17.
 */
public class GetMetricDataMsg extends NeedReplyMessage {
    private String namespace;

    private String metricName;

    private Long startTime;

    private Long endTime;

    private Long offsetAheadOfCurrentTime;

    private Integer period;

    private List<String> labels;

    private List<String> functions;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Long getOffsetAheadOfCurrentTime() {
        return offsetAheadOfCurrentTime;
    }

    public void setOffsetAheadOfCurrentTime(Long offsetAheadOfCurrentTime) {
        this.offsetAheadOfCurrentTime = offsetAheadOfCurrentTime;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<String> getFunctions() {
        return functions;
    }

    public void setFunctions(List<String> functions) {
        this.functions = functions;
    }
}

package org.zstack.zwatch.datatype;

import java.util.List;

public class LabelValueQueryObject {
    private String namespaceName;
    private String metricName;
    private String accountUuid;
    private Long startTime;
    private Long endTime;
    private List<String> labelNames;
    private List<Label> filteredLabels;
    private List<Label> filters;

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

    public String getNamespaceName() {
        return namespaceName;
    }

    public void setNamespaceName(String namespaceName) {
        this.namespaceName = namespaceName;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public List<String> getLabelNames() {
        return labelNames;
    }

    public void setLabelNames(List<String> labelNames) {
        this.labelNames = labelNames;
    }

    public List<Label> getFilteredLabels() {
        return filteredLabels;
    }

    public void setFilteredLabels(List<Label> filteredLabels) {
        this.filteredLabels = filteredLabels;
    }

    public List<Label> getFilters() {
        return filters;
    }

    public void setFilters(List<Label> filters) {
        this.filters = filters;
    }
}

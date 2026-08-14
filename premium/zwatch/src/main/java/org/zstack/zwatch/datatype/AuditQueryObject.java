package org.zstack.zwatch.datatype;

import java.util.ArrayList;
import java.util.List;

public class AuditQueryObject implements QueryObject {
    private Long startTime;
    private Long endTime;
    private List<Label> labels = new ArrayList<>();
    private int limit = 1000;

    public AuditQueryObject(AuditQueryObject other) {
        this.startTime = other.startTime;
        this.endTime = other.endTime;
        this.labels = other.labels;
        this.limit = other.limit;
    }

    public AuditQueryObject() {
    }

    public static class Builder {
        private Long startTime;
        private Long endTime;
        private List<Label> labels;
        private Integer limit;

        public Builder startTime(Long v) {
            startTime = v;
            return this;
        }

        public Builder endTime(Long v) {
            endTime = v;
            return this;
        }

        public Builder labels(List<Label> v) {
            labels = v;
            return this;
        }

        public Builder label(String k, String v) {
            if (labels == null) {
                labels = new ArrayList<>();
            }

            labels.add(new Label(k, v));
            return this;
        }

        public Builder limit(int v) {
            limit = v;
            return this;
        }

        public AuditQueryObject build() {
            AuditQueryObject obj = new AuditQueryObject();
            obj.startTime = startTime;
            obj.endTime = endTime;
            if (labels != null) {
                obj.labels = labels;
            } else {
                obj.labels = new ArrayList<>();
            }
            if (limit != null) {
                obj.limit = limit;
            }

            return obj;
        }
    }

    public static Builder New() {
        return new Builder();
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

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}

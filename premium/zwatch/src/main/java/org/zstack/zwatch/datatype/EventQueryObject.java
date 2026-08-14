package org.zstack.zwatch.datatype;

import org.springframework.security.access.method.P;

import java.util.ArrayList;
import java.util.List;

public class EventQueryObject implements QueryObject {
    private Long startTime;
    private Long endTime;
    private List<Label> labels = new ArrayList<>();
    private int limit = 1000;
    private int offset = 0;
    private Long time;
    private List<String> whereConditions = new ArrayList<>();

    public EventQueryObject(EventQueryObject other) {
        this.startTime = other.startTime;
        this.endTime = other.endTime;
        this.labels = other.labels;
        this.limit = other.limit;
        this.offset = other.offset;
        this.time = other.time;
        this.whereConditions = other.whereConditions;
    }

    public EventQueryObject() {
    }

    public static class Builder {
        private Long startTime;
        private Long endTime;
        private Long time;
        private List<Label> labels;
        private Integer limit;
        private Integer offset;
        private List<String> whereConditions;
        private String accountUuid;

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

        public Builder offset(int v) {
            offset = v;
            return this;
        }

        public Builder noLimit() {
            limit = -1;
            return this;
        }

        public Builder time(long v) {
            time = v;
            return this;
        }

        public Builder whereConditions(List<String> v) {
            if (v == null || v.size() == 0) {
                return this;
            }

            whereConditions = v;
            return this;
        }

        public Builder account(String v) {
            accountUuid = v;
            return this;
        }

        public EventQueryObject build() {
            EventQueryObject obj = new EventQueryObject();
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
            if (offset != null) {
                obj.offset = offset;
            } else {
                obj.offset = 0;
            }
            if (time != null) {
                obj.time = time;
            }
            if (whereConditions != null) {
                obj.whereConditions = whereConditions;
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

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public List<String> getWhereConditions() {
        return whereConditions;
    }

    public void setWhereConditions(List<String> whereConditions) {
        this.whereConditions = whereConditions;
    }
}

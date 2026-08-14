package org.zstack.header.message;

import java.util.List;

public abstract class APIGetMessage extends APISyncCallMessage {
    private Integer limit = 1000;
    private Integer start = 0;

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public<T> List<T> filter(List<T> candidates) {
        if (candidates.isEmpty() || candidates.size() < start) {
            return candidates;
        }
        Integer end = start+limit;
        if (end >= candidates.size()) {
            end = candidates.size();
        }
        return candidates.subList(start, end);
    }
}

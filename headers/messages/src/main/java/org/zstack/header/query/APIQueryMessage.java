package org.zstack.header.query;

import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;

import java.util.ArrayList;
import java.util.List;

public abstract class APIQueryMessage extends APISyncCallMessage {
    @APIParam
    private List<QueryCondition> conditions;
    private Integer limit = 1000;
    private Integer start;
    private boolean count;
    private String groupBy;
    private boolean replyWithCount;
    private String filterName;
    private String sortBy;
    @APIParam(required = false, validValues = {"asc", "desc"})
    private String sortDirection = "asc";
    private List<String> fields;

    public String getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public List<QueryCondition> getConditions() {
        if (conditions == null) {
            conditions = new ArrayList<QueryCondition>();
        }
        return conditions;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    public boolean isReplyWithCount() {
        return replyWithCount;
    }

    public void setReplyWithCount(boolean replyWithCount) {
        this.replyWithCount = replyWithCount;
    }

    public void setConditions(List<QueryCondition> conditions) {
        this.conditions = conditions;
    }

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

    public void addQueryCondition(String name, String op, String... vals) {
        QueryCondition qc = new QueryCondition();
        qc.setName(name);
        qc.setOp(op);
        if (vals.length == 1) {
            qc.setValue(vals[0]);
        } else {
            qc.setValues(vals);
        }
        getConditions().add(qc);
    }

    // internal use for test
    public void addQueryCondition(String name, QueryOp op, String... vals) {
        addQueryCondition(name, op.toString(), vals);
    }

    public void addField(String name) {
        if (fields == null) {
            fields = new ArrayList<String>();
        }
        fields.add(name);
    }

    public boolean isFieldQuery() {
        return fields != null && !fields.isEmpty();
    }

    public boolean isCount() {
        return count;
    }

    public void setCount(boolean count) {
        this.count = count;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }
}

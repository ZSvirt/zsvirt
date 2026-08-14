package org.zstack.header.query;

import org.apache.commons.lang.StringUtils;

public class QueryCondition {
    private String name;
    private String op;
    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setValues(String... values) {
        value = StringUtils.join(values, ",");
    }
}

package org.zstack.zwatch.mysql;

import java.util.Map;

public class MysqlQueryExpression {
    private Map<String, String> labels;
    private MysqlExpressionType type;

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public MysqlExpressionType getType() {
        return type;
    }

    public void setType(MysqlExpressionType type) {
        this.type = type;
    }
}

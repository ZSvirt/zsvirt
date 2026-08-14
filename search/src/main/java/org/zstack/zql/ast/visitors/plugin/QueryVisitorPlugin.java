package org.zstack.zql.ast.visitors.plugin;

import org.zstack.header.zql.ASTNode;

import java.util.List;

public abstract class QueryVisitorPlugin {
    public enum ClauseType {
        QUERY,
        SUM,
        COUNT,
        SIMPLE_COUNT,
    }

    protected ASTNode.Query node;

    public QueryVisitorPlugin() {
    }

    public QueryVisitorPlugin(ASTNode.Query node) {
        this.node = node;
    }

    public abstract ClauseType getClauseType();
    public abstract String selectTarget();
    public abstract List<String> targetFields();
    public abstract String functions();
    public abstract String tableName();
    public abstract String conditions();
    public abstract String restrictBy();
    public abstract String orderBy();
    public abstract Integer limit();
    public abstract Integer offset();
    public abstract String groupBy();
}

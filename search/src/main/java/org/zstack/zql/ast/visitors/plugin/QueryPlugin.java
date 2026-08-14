package org.zstack.zql.ast.visitors.plugin;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.zql.ASTNode;

import java.util.List;
import java.util.stream.Collectors;

public class QueryPlugin extends AbstractQueryVisitorPlugin {
    public QueryPlugin() {
    }

    public QueryPlugin(ASTNode.Query node) {
        super(node);
    }

    @Override
    public String selectTarget() {
        String queryTarget;
        List<String> fieldNames = targetFields();

        if (fieldNames.isEmpty()) {
            queryTarget = entityAlias;
        } else {
            List<String> qt = fieldNames.stream().map(f->String.format("%s.%s", inventory.simpleInventoryName(), f)).collect(Collectors.toList());
            queryTarget = StringUtils.join(qt, ",");
        }

        return String.format(functions(), queryTarget);
    }


    @Override
    public ClauseType getClauseType() {
        return ClauseType.QUERY;
    }
}

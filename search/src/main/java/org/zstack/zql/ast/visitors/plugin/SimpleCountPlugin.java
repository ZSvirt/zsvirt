package org.zstack.zql.ast.visitors.plugin;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.zql.ASTNode;

import java.util.List;
import java.util.stream.Collectors;

public class SimpleCountPlugin extends AbstractQueryVisitorPlugin {
    public SimpleCountPlugin() {
    }

    public SimpleCountPlugin(ASTNode.Query node) {
        super(node);
    }

    @Override
    public ClauseType getClauseType() {
        return ClauseType.SIMPLE_COUNT;
    }

    @Override
    public String selectTarget() {
        String queryTarget;
        List<String> fieldNames = super.targetFields();

        if (fieldNames.isEmpty()) {
            queryTarget = entityAlias;
        } else {
            // TODO: Compatibility changes: hql count do not support multiple fields, even if distinct modified.
            queryTarget = String.format("%s.%s", inventory.simpleInventoryName(), fieldNames.get(0));
        }

        return String.format("count(%s)", String.format(functions(), queryTarget));
    }
}

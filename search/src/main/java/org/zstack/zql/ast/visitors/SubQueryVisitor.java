package org.zstack.zql.ast.visitors;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ast.ZQLMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SubQueryVisitor implements ASTVisitor<String, ASTNode.SubQuery> {
    private String makeConditions(ASTNode.SubQuery node) {
        if (node.getConditions() == null || node.getConditions().isEmpty()) {
            return "";
        }

        List<String> conds = node.getConditions().stream().map(it -> (String)((ASTNode)it).accept(new ConditionVisitor())).collect(Collectors.toList());
        return StringUtils.join(conds, " ");
    }

    @Override
    public String visit(ASTNode.SubQuery node) {
        ZQLMetadata.InventoryMetadata inventory = ZQLMetadata.findInventoryMetadata(node.getTarget().getEntity());
        ZQLContext.pushQueryTargetInventoryName(inventory.fullInventoryName());

        String fieldName = node.getTarget().getFields() == null || node.getTarget().getFields().isEmpty() ? "" : node.getTarget().getFields().get(0);
        if (!"".equals(fieldName)) {
            inventory.errorIfNoField(fieldName);
        }

        String entityAlias = inventory.simpleInventoryName();
        String queryTarget = fieldName.equals("") ? entityAlias : String.format("%s.%s", inventory.simpleInventoryName(), fieldName);
        String entityVOName = inventory.inventoryAnnotation.mappingVOClass().getSimpleName();

        List<String> clauses = new ArrayList<>();
        clauses.add(String.format("SELECT %s FROM %s %s", queryTarget, entityVOName, entityAlias));
        String condition = makeConditions(node);
        if (!condition.equals("")) {
            clauses.add("WHERE");
            clauses.add(condition);
        }

        ZQLContext.popQueryTargetInventoryName();
        return String.format("(%s)", StringUtils.join(clauses, " "));
    }
}

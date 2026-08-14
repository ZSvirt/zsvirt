package org.zstack.zql.ast.visitors.plugin;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.zql.ASTNode;
import org.zstack.zql.ast.ZQLMetadata;
import org.zstack.zql.ast.visitors.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractQueryVisitorPlugin extends QueryVisitorPlugin {
    protected ZQLMetadata.InventoryMetadata inventory;
    protected String entityAlias;
    protected String entityVOName;

    public AbstractQueryVisitorPlugin() {
    }

    public AbstractQueryVisitorPlugin(ASTNode.Query node) {
        super(node);
        inventory = ZQLMetadata.findInventoryMetadata(node.getTarget().getEntity());
        entityAlias = inventory.simpleInventoryName();
        entityVOName = inventory.inventoryAnnotation.mappingVOClass().getSimpleName();
    }

    @Override
    public List<String> targetFields() {
        ZQLMetadata.InventoryMetadata inventory = ZQLMetadata.findInventoryMetadata(node.getTarget().getEntity());
        List<String> fieldNames = node.getTarget().getFields() == null ? new ArrayList<>() : node.getTarget().getFields();
        fieldNames.forEach(inventory::errorIfNoField);
        return fieldNames;
    }

    @Override
    public String functions() {
        String functions = "%s";
        ASTNode.Function function;
        ASTNode.QueryTargetWithFunction target = node.getTarget();
        while ((function = target.getFunction()) != null) {
            functions = String.format(functions, ((ASTNode) function).accept(new FunctionVisitor()));
            if ((target = target.getSubTarget()) == null) {
                break;
            }
        }
        return functions;
    }

    @Override
    public String tableName() {
        return String.format("%s %s", entityVOName, entityAlias);
    }

    @Override
    public String conditions() {
        if (node.getConditions() == null || node.getConditions().isEmpty()) {
            return "";
        }

        List<String> conds = node.getConditions().stream().map(it->(String)((ASTNode)it).accept(new ConditionVisitor())).collect(Collectors.toList());
        return StringUtils.join(conds, " ");
    }

    @Override
    public String restrictBy() {
        return node.getRestrictBy() == null ? null : (String) node.getRestrictBy().accept(new RestrictByVisitor());
    }

    @Override
    public String orderBy() {
        return node.getOrderBy() == null ? null : (String) node.getOrderBy().accept(new OrderByVisitor());
    }

    @Override
    public Integer limit() {
        return node.getLimit() == null ? null : (Integer) node.getLimit().accept(new LimitVisitor());
    }

    @Override
    public Integer offset() {
        return node.getOffset() == null ? null : (Integer) node.getOffset().accept(new OffsetVisitor());
    }

    @Override
    public String groupBy() {
        if (node.getGroupBy() == null) {
            return null;
        }

        List<String> fs = node.getGroupBy().getFields();
        ZQLMetadata.InventoryMetadata inventory = ZQLMetadata.findInventoryMetadata(node.getTarget().getEntity());
        fs.forEach(inventory::errorIfNoField);
        return node.getGroupBy() == null ? null : (String) node.getGroupBy().accept(new GroupByVisitor());
    }
}

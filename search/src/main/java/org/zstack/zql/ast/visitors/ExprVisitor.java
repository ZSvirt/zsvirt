package org.zstack.zql.ast.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.sql.SQLConditionBuilder;

public class ExprVisitor implements ASTVisitor<String, ASTNode.Expr> {
    @Override
    public String visit(ASTNode.Expr node) {
        String inventoryTarget = ZQLContext.peekQueryTargetInventoryName();
        return new SQLConditionBuilder(inventoryTarget, node.getLeft(), node.getOperator(),
                node.getRight() == null ? "" : (String) ((ASTNode)node.getRight()).accept(new ValueVisitor())).build();
    }
}

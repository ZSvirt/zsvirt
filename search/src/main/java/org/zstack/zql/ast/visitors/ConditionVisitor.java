package org.zstack.zql.ast.visitors;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;
import org.zstack.utils.DebugUtils;

public class ConditionVisitor implements ASTVisitor<String, ASTNode> {
    @Override
    public String visit(ASTNode node) {
        DebugUtils.Assert(node instanceof ASTNode.Condition, "node not instanceof ASTNode.Condition");

        if (node instanceof ASTNode.Expr) {
            return (String) node.accept(new ExprVisitor());
        } else if (node instanceof ASTNode.LogicalOperator) {
            return (String) node.accept(new LogicalOperatorVisitor());
        } else {
            throw new CloudRuntimeException("should not be here");
        }
    }
}

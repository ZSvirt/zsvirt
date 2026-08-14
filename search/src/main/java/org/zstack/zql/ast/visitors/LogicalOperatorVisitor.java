package org.zstack.zql.ast.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;

public class LogicalOperatorVisitor implements ASTVisitor<String, ASTNode.LogicalOperator> {
    @Override
    public String visit(ASTNode.LogicalOperator node) {
        String left = (String) ((ASTNode)node.getLeft()).accept(new ConditionVisitor());
        String right = (String) ((ASTNode)node.getRight()).accept(new ConditionVisitor());
        return String.format("(%s %s %s)", left, node.getOperator().toUpperCase(), right);
    }
}

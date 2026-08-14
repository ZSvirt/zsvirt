package org.zstack.zql.ast.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;

public class ComplexValueVisitor implements ASTVisitor<String, ASTNode.ComplexValue> {
    @Override
    public String visit(ASTNode.ComplexValue node) {
        return (String) node.getSubQuery().accept(new SubQueryVisitor());
    }
}

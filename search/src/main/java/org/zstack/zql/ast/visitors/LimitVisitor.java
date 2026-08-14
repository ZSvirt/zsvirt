package org.zstack.zql.ast.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;

public class LimitVisitor implements ASTVisitor<Integer, ASTNode.Limit> {
    @Override
    public Integer visit(ASTNode.Limit node) {
        return node.getLimit();
    }
}

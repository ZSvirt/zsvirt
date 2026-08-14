package org.zstack.zql.ast.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;

public class OffsetVisitor implements ASTVisitor<Integer, ASTNode.Offset> {
    @Override
    public Integer visit(ASTNode.Offset node) {
        return node.getOffset();
    }
}

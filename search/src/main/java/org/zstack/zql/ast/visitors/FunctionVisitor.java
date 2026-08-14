package org.zstack.zql.ast.visitors;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;

/**
 * Created by MaJin on 2019/6/3.
 */
public class FunctionVisitor implements ASTVisitor<String, ASTNode> {
    @Override
    public String visit(ASTNode node) {
        if (node instanceof ASTNode.Distinct) {
            return (String) node.accept(new DistinctVistor());
        } else {
            throw new CloudRuntimeException(String.format("should not be here, %s", node.getClass()));
        }
    }
}

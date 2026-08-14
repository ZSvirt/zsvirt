package org.zstack.zql.ast.visitors;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;
import org.zstack.zql.ast.visitors.result.ReturnWithResult;

public class ReturnWithExprVisitor implements ASTVisitor<ReturnWithResult, ASTNode> {
    @Override
    public ReturnWithResult visit(ASTNode node) {
        if (node instanceof ASTNode.ReturnWithIDExpr) {
            ReturnWithResult r = new ReturnWithResult();
            r.name = StringUtils.join(((ASTNode.ReturnWithIDExpr) node).getNames(), ".");
            return r;
        } else if (node instanceof ASTNode.ReturnWithBlockExpr) {
            ReturnWithResult r = new ReturnWithResult();
            r.name = ((ASTNode.ReturnWithBlockExpr) node).getName();
            r.expr = ((ASTNode.ReturnWithBlockExpr) node).getContent();
            return r;
        } else {
            throw new CloudRuntimeException("should not be here");
        }
    }
}

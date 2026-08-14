package org.zstack.zql.ast.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;
import org.zstack.zql.ast.visitors.result.ReturnWithResult;

import java.util.List;
import java.util.stream.Collectors;

public class ReturnWithVisitor implements ASTVisitor<List<ReturnWithResult>, ASTNode.ReturnWith> {
    @Override
    public List<ReturnWithResult> visit(ASTNode.ReturnWith node) {
        return node.getExprs().stream().map(it->(ReturnWithResult)((ASTNode)it).accept(new ReturnWithExprVisitor())).collect(Collectors.toList());
    }
}

package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

public class RestrictByVisitor extends ZQLBaseVisitor<ASTNode.RestrictBy> {
    @Override
    public ASTNode.RestrictBy visitRestrictBy(ZQLParser.RestrictByContext ctx) {
        ASTNode.RestrictBy r = new ASTNode.RestrictBy();
        r.setExprs(ctx.restrictByExpr().stream().map(it->it.accept(new RestrictExprVisitor())).collect(Collectors.toList()));
        return r;
    }
}

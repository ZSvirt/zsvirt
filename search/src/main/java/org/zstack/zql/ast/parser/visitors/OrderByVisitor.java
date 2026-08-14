package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

public class OrderByVisitor extends ZQLBaseVisitor<ASTNode.OrderBy> {
    @Override
    public ASTNode.OrderBy visitOrderBy(ZQLParser.OrderByContext ctx) {
        ASTNode.OrderBy o = new ASTNode.OrderBy();
        o.setExprs(ctx.orderByExpr().stream().map(it->it.accept(new OrderByExprVisitor())).collect(Collectors.toList()));
        return o;
    }
}

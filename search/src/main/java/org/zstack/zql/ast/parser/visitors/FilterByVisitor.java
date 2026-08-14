package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

public class FilterByVisitor extends ZQLBaseVisitor<ASTNode.FilterBy> {
    @Override
    public ASTNode.FilterBy visitFilterBy(ZQLParser.FilterByContext ctx) {
        ASTNode.FilterBy f = new ASTNode.FilterBy();
        f.setExprs(ctx.filterByExpr().stream().map(it->it.accept(new FilterByExprVisitor())).collect(Collectors.toList()));
        return f;
    }
}

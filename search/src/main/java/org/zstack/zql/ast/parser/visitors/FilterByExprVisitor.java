package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

public class FilterByExprVisitor extends ZQLBaseVisitor<ASTNode.FilterByExpr> {
    @Override
    public ASTNode.FilterByExpr visitFilterByExpr(ZQLParser.FilterByExprContext ctx) {
        ASTNode.FilterByExpr f = new ASTNode.FilterByExpr();
        f.setFilterName(ctx.ID().getText());
        f.setContent(ctx.filterByExprBlock().getText());
        return f;
    }
}

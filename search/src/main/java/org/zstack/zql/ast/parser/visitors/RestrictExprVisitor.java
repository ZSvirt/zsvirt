package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

public class RestrictExprVisitor extends ZQLBaseVisitor<ASTNode.RestrictExpr> {
    @Override
    public ASTNode.RestrictExpr visitRestrictByExpr(ZQLParser.RestrictByExprContext ctx) {
        ASTNode.RestrictExpr e = new ASTNode.RestrictExpr();
        e.setEntity(ctx.entity() == null ? null : ctx.entity().getText());
        e.setField(ctx.ID().getText());
        e.setOperator(ctx.operator().getText());
        e.setValue(ctx.value() == null ? null : ctx.value().accept(new ValueVisitor()));
        return e;
    }
}

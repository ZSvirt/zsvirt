package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

/**
 * Created by MaJin on 2020/4/29.
 */
public class OrderByExprVisitor extends ZQLBaseVisitor<ASTNode.OrderByExpr> {
    @Override
    public ASTNode.OrderByExpr visitOrderByExpr(ZQLParser.OrderByExprContext ctx) {
        ASTNode.OrderByExpr o = new ASTNode.OrderByExpr();
        o.setDirection(ctx.ORDER_BY_VALUE().getText());
        o.setTarget(ctx.exprAtom().accept(new ExprAtomVisitor()));
        return o;
    }
}

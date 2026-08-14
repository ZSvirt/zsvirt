package org.zstack.zql.ast.parser.visitors;

import org.antlr.v4.runtime.tree.ParseTree;
import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

public class ExprVisitor extends ZQLBaseVisitor<ASTNode.Expr> {
    @Override
    public ASTNode.Expr visitExpr(ZQLParser.ExprContext ctx) {
        ASTNode.Expr e = new ASTNode.Expr();
        e.setLeft(ctx.field().ID().stream().map(ParseTree::getText).collect(Collectors.toList()));
        e.setRight(ctx.complexValue() == null ? null : ctx.complexValue().accept(new ValueVisitor()));
        e.setOperator(ctx.operator().getText());
        return e;
    }
}

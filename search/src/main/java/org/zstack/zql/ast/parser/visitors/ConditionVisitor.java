package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

public class ConditionVisitor extends ZQLBaseVisitor<ASTNode.Condition> {
    @Override
    public ASTNode.Condition visitNestCondition(ZQLParser.NestConditionContext ctx) {
        ASTNode.LogicalOperator l = new ASTNode.LogicalOperator();
        l.setLeft(ctx.left.accept(new ConditionVisitor()));
        l.setOperator(ctx.op == null ? null : ctx.op.getText());
        l.setRight(ctx.right == null ? null : ctx.right.accept(new ConditionVisitor()));
        return l;
    }

    @Override
    public ASTNode.Condition visitSimpleCondition(ZQLParser.SimpleConditionContext ctx) {
        return ctx.expr().accept(new ExprVisitor());
    }

    @Override
    public ASTNode.Condition visitParenthesisCondition(ZQLParser.ParenthesisConditionContext ctx) {
        return ctx.condition().accept(new ConditionVisitor());
    }
}

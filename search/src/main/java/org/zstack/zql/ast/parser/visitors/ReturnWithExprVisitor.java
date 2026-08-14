package org.zstack.zql.ast.parser.visitors;

import org.antlr.v4.runtime.tree.ParseTree;
import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

public class ReturnWithExprVisitor extends ZQLBaseVisitor<ASTNode.ReturnWithExpr> {
    @Override
    public ASTNode.ReturnWithIDExpr visitReturnWithExprId(ZQLParser.ReturnWithExprIdContext ctx) {
        ASTNode.ReturnWithIDExpr r = new ASTNode.ReturnWithIDExpr();
        r.setNames(ctx.ID().stream().map(ParseTree::getText).collect(Collectors.toList()));
        return r;
    }

    @Override
    public ASTNode.ReturnWithBlockExpr visitReturnWithExprFunction(ZQLParser.ReturnWithExprFunctionContext ctx) {
        ASTNode.ReturnWithBlockExpr r = new ASTNode.ReturnWithBlockExpr();
        r.setName(ctx.ID().getText());
        r.setContent(ctx.returnWithExprBlock().getText());
        return r;
    }
}

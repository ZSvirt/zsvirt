package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

public class ReturnWithVisitor  extends ZQLBaseVisitor<ASTNode.ReturnWith> {
    @Override
    public ASTNode.ReturnWith visitReturnWith(ZQLParser.ReturnWithContext ctx) {
        ASTNode.ReturnWith r = new ASTNode.ReturnWith();
        r.setExprs(ctx.returnWithExpr().stream().map(it->it.accept(new ReturnWithExprVisitor())).collect(Collectors.toList()));
        return r;
    }
}

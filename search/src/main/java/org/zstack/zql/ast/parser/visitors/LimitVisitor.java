package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

public class LimitVisitor extends ZQLBaseVisitor<ASTNode.Limit> {
    @Override
    public ASTNode.Limit visitLimit(ZQLParser.LimitContext ctx) {
        ASTNode.Limit l = new ASTNode.Limit();
        l.setLimit(Integer.parseInt(ctx.INT().getText()));
        return l;
    }
}

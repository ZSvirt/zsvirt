package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

public class OffsetVisitor extends ZQLBaseVisitor<ASTNode.Offset> {
    @Override
    public ASTNode.Offset visitOffset(ZQLParser.OffsetContext ctx) {
        ASTNode.Offset o = new ASTNode.Offset();
        o.setOffset(Long.valueOf(ctx.INT().getText()).intValue());
        return o;
    }
}

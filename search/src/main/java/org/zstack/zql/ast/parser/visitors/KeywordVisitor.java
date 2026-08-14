package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

public class KeywordVisitor extends ZQLBaseVisitor<ASTNode.Keyword> {
    @Override
    public ASTNode.Keyword visitKeyword(ZQLParser.KeywordContext ctx) {
        ASTNode.Keyword r = new ASTNode.Keyword();
        r.setValue(ctx.getText());
        return r;
    }
}

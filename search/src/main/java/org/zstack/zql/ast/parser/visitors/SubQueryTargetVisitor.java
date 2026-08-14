package org.zstack.zql.ast.parser.visitors;

import org.antlr.v4.runtime.tree.ParseTree;
import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

public class SubQueryTargetVisitor extends ZQLBaseVisitor<ASTNode.SubQueryTarget> {
    @Override
    public ASTNode.SubQueryTarget visitSubQueryTarget(ZQLParser.SubQueryTargetContext ctx) {
        ASTNode.SubQueryTarget t = new ASTNode.SubQueryTarget();
        t.setEntity(ctx.entity().getText());
        t.setFields(ctx.ID().stream().map(ParseTree::getText).collect(Collectors.toList()));
        return t;
    }
}

package org.zstack.zql.ast.parser.visitors;

import org.antlr.v4.runtime.tree.ParseTree;
import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.list;

public class QueryTargetVisitor extends ZQLBaseVisitor<ASTNode.QueryTarget> {
    @Override
    public ASTNode.QueryTarget visitOnlyEntity(ZQLParser.OnlyEntityContext ctx) {
        ASTNode.QueryTarget q = new ASTNode.QueryTarget();
        q.setEntity(ctx.entity().ID().getText());
        return q;
    }

    @Override
    public ASTNode.QueryTarget visitWithSingleField(ZQLParser.WithSingleFieldContext ctx) {
        ASTNode.QueryTarget q = new ASTNode.QueryTarget();
        q.setEntity(ctx.entity().ID().getText());
        q.setFields(list(ctx.field().ID().get(0).getText()));
        return q;
    }

    @Override
    public ASTNode.QueryTarget visitWithMultiFields(ZQLParser.WithMultiFieldsContext ctx) {
        ASTNode.QueryTarget q = new ASTNode.QueryTarget();
        q.setEntity(ctx.entity().ID().getText());
        q.setFields(ctx.multiFields().ID().stream().map(ParseTree::getText).collect(Collectors.toList()));
        return q;
    }
}

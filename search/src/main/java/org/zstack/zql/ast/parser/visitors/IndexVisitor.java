package org.zstack.zql.ast.parser.visitors;

import org.antlr.v4.runtime.tree.ParseTree;
import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.list;

public class IndexVisitor extends ZQLBaseVisitor<ASTNode.Index> {
    @Override
    public ASTNode.Index visitSingleIndex(ZQLParser.SingleIndexContext ctx) {
        ASTNode.Index r = new ASTNode.Index();
        r.setIndexs(list(ctx.ID().getText()));
        return r;
    }

    @Override
    public ASTNode.Index visitMultiIndexs(ZQLParser.MultiIndexsContext ctx) {
        ASTNode.Index r = new ASTNode.Index();
        r.setIndexs(ctx.ID().stream().map(ParseTree::getText).collect(Collectors.toList()));
        return r;
    }
}

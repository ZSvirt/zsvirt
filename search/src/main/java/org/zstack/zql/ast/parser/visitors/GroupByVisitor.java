package org.zstack.zql.ast.parser.visitors;

import org.antlr.v4.runtime.tree.ParseTree;
import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

public class GroupByVisitor extends ZQLBaseVisitor<ASTNode.GroupByExpr> {
    @Override
    public ASTNode.GroupByExpr visitGroupBy(ZQLParser.GroupByContext ctx) {
        ASTNode.GroupByExpr ret = new ASTNode.GroupByExpr();
        ret.setFields(ctx.groupByExpr().ID().stream().map(ParseTree::getText).collect(Collectors.toList()));
        return ret;
    }
}

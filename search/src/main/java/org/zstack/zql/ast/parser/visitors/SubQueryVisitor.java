package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

public class SubQueryVisitor extends ZQLBaseVisitor<ASTNode.SubQuery> {
    public ASTNode.SubQuery visitSubQuery(ZQLParser.SubQueryContext ctx) {
        ASTNode.SubQuery s = new ASTNode.SubQuery();
        s.setTarget(ctx.subQueryTarget().accept(new SubQueryTargetVisitor()));
        s.setConditions(ctx.condition().stream().map(it->it.accept(new ConditionVisitor())).collect(Collectors.toList()));
        return s;
    }
}

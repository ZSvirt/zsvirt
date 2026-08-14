package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

public class SumVisitor extends ZQLBaseVisitor<ASTNode.Sum> {
    @Override
    public ASTNode.Sum visitSum(ZQLParser.SumContext ctx) {
        ASTNode.Sum sum = new ASTNode.Sum();
        sum.setTarget(ASTNode.QueryTargetWithFunction.valueOf(ctx.queryTarget().accept(new QueryTargetVisitor())));
        sum.setConditions(ctx.condition() == null ? null : ctx.condition().stream().map(c->c.accept(new ConditionVisitor())).collect(Collectors.toList()));
        sum.setGroupByField(ctx.sumBy().sumByValue().getText());
        sum.setOrderBy(ctx.orderBy() == null ? null : ctx.orderBy().accept(new OrderByVisitor()));
        sum.setLimit(ctx.limit() == null ? null : ctx.limit().accept(new LimitVisitor()));
        sum.setOffset(ctx.offset() == null ? null : ctx.offset().accept(new OffsetVisitor()));
        sum.setName(ctx.namedAs() == null ? null : ctx.namedAs().accept(new NamedAsVisitor()));
        return sum;
    }
}

package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

public class QueryVisitor extends ZQLBaseVisitor<ASTNode.Query> {
    @Override
    public ASTNode.Query visitQuery(ZQLParser.QueryContext ctx) {
        ASTNode.Query  q = new ASTNode.Query();
        q.setTarget(ctx.queryTargetWithFunction().accept(new QueryTargetWithFunctionVisitor()));
        q.setConditions(ctx.condition() == null ? null : ctx.condition().stream().map(it->it.accept(new ConditionVisitor())).collect(Collectors.toList()));
        q.setFilterBy(ctx.filterBy() == null ? null : ctx.filterBy().accept(new FilterByVisitor()));
        q.setReturnWith(ctx.returnWith() == null ? null : ctx.returnWith().accept(new ReturnWithVisitor()));
        q.setRestrictBy(ctx.restrictBy() == null ? null : ctx.restrictBy().accept(new RestrictByVisitor()));
        q.setOrderBy(ctx.orderBy() == null ? null : ctx.orderBy().accept(new OrderByVisitor()));
        q.setLimit(ctx.limit() == null ? null : ctx.limit().accept(new LimitVisitor()));
        q.setOffset(ctx.offset() == null ? null : ctx.offset().accept(new OffsetVisitor()));
        q.setGroupBy(ctx.groupBy() == null ? null : ctx.groupBy().accept(new GroupByVisitor()));
        q.setName(ctx.namedAs() == null ? null : ctx.namedAs().accept(new NamedAsVisitor()));
        return q;
    }
}

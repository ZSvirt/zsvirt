package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

/**
 * @ Author : yh.w
 * @ Date   : Created in 15:07 2020/10/27
 */
public class SearchVisitor extends ZQLBaseVisitor<ASTNode.Search> {
    @Override
    public ASTNode.Search visitSearch(ZQLParser.SearchContext ctx) {
        ASTNode.Search search = new ASTNode.Search();
        search.setKeyword(ctx.keyword().accept(new KeywordVisitor()));
        search.setIndex(ctx.index() == null ? null : ctx.index().accept(new IndexVisitor()));
        search.setLimit(ctx.limit() == null ? null : ctx.limit().accept(new LimitVisitor()));
        search.setOffset(ctx.offset() == null ? null : ctx.offset().accept(new OffsetVisitor()));
        search.setRestrictBy(ctx.restrictBy() == null ? null : ctx.restrictBy().accept(new RestrictByVisitor()));
        return search;
    }
}

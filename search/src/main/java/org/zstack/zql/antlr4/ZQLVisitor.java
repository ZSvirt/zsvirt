// Generated from ZQL.g4 by ANTLR 4.7

package org.zstack.zql.antlr4;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ZQLParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ZQLVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ZQLParser#zqls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitZqls(ZQLParser.ZqlsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code queryGrammar}
	 * labeled alternative in {@link ZQLParser#zql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryGrammar(ZQLParser.QueryGrammarContext ctx);
	/**
	 * Visit a parse tree produced by the {@code countGrammar}
	 * labeled alternative in {@link ZQLParser#zql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCountGrammar(ZQLParser.CountGrammarContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sumGrammar}
	 * labeled alternative in {@link ZQLParser#zql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSumGrammar(ZQLParser.SumGrammarContext ctx);
	/**
	 * Visit a parse tree produced by the {@code searchGrammar}
	 * labeled alternative in {@link ZQLParser#zql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearchGrammar(ZQLParser.SearchGrammarContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#entity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntity(ZQLParser.EntityContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#field}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField(ZQLParser.FieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#multiFields}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiFields(ZQLParser.MultiFieldsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperator(ZQLParser.OperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(ZQLParser.ValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#listValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListValue(ZQLParser.ListValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#logicalOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalOperator(ZQLParser.LogicalOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code simpleValue}
	 * labeled alternative in {@link ZQLParser#complexValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleValue(ZQLParser.SimpleValueContext ctx);
	/**
	 * Visit a parse tree produced by the {@code subQueryValue}
	 * labeled alternative in {@link ZQLParser#complexValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubQueryValue(ZQLParser.SubQueryValueContext ctx);
	/**
	 * Visit a parse tree produced by the {@code apiGetValue}
	 * labeled alternative in {@link ZQLParser#complexValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitApiGetValue(ZQLParser.ApiGetValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#getQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGetQuery(ZQLParser.GetQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#apiparams}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitApiparams(ZQLParser.ApiparamsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#input}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInput(ZQLParser.InputContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#output}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOutput(ZQLParser.OutputContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(ZQLParser.ExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code columnNameExprAtom}
	 * labeled alternative in {@link ZQLParser#exprAtom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnNameExprAtom(ZQLParser.ColumnNameExprAtomContext ctx);
	/**
	 * Visit a parse tree produced by the {@code mathExprAtom}
	 * labeled alternative in {@link ZQLParser#exprAtom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMathExprAtom(ZQLParser.MathExprAtomContext ctx);
	/**
	 * Visit a parse tree produced by the {@code nestedExprAtom}
	 * labeled alternative in {@link ZQLParser#exprAtom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNestedExprAtom(ZQLParser.NestedExprAtomContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#equal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqual(ZQLParser.EqualContext ctx);
	/**
	 * Visit a parse tree produced by the {@code nestCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNestCondition(ZQLParser.NestConditionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code simpleCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleCondition(ZQLParser.SimpleConditionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parenthesisCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesisCondition(ZQLParser.ParenthesisConditionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code onlyEntity}
	 * labeled alternative in {@link ZQLParser#queryTarget}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOnlyEntity(ZQLParser.OnlyEntityContext ctx);
	/**
	 * Visit a parse tree produced by the {@code withSingleField}
	 * labeled alternative in {@link ZQLParser#queryTarget}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWithSingleField(ZQLParser.WithSingleFieldContext ctx);
	/**
	 * Visit a parse tree produced by the {@code withMultiFields}
	 * labeled alternative in {@link ZQLParser#queryTarget}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWithMultiFields(ZQLParser.WithMultiFieldsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction(ZQLParser.FunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code withoutFunction}
	 * labeled alternative in {@link ZQLParser#queryTargetWithFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWithoutFunction(ZQLParser.WithoutFunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code withFunction}
	 * labeled alternative in {@link ZQLParser#queryTargetWithFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWithFunction(ZQLParser.WithFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#orderByExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderByExpr(ZQLParser.OrderByExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#orderBy}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderBy(ZQLParser.OrderByContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#limit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLimit(ZQLParser.LimitContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#offset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOffset(ZQLParser.OffsetContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#restrictByExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRestrictByExpr(ZQLParser.RestrictByExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#restrictBy}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRestrictBy(ZQLParser.RestrictByContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#returnWithExprBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnWithExprBlock(ZQLParser.ReturnWithExprBlockContext ctx);
	/**
	 * Visit a parse tree produced by the {@code returnWithExprId}
	 * labeled alternative in {@link ZQLParser#returnWithExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnWithExprId(ZQLParser.ReturnWithExprIdContext ctx);
	/**
	 * Visit a parse tree produced by the {@code returnWithExprFunction}
	 * labeled alternative in {@link ZQLParser#returnWithExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnWithExprFunction(ZQLParser.ReturnWithExprFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#returnWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnWith(ZQLParser.ReturnWithContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#groupByExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupByExpr(ZQLParser.GroupByExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#groupBy}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupBy(ZQLParser.GroupByContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#subQueryTarget}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubQueryTarget(ZQLParser.SubQueryTargetContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#subQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubQuery(ZQLParser.SubQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#filterByExprBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterByExprBlock(ZQLParser.FilterByExprBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#filterByExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterByExpr(ZQLParser.FilterByExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#filterBy}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterBy(ZQLParser.FilterByContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#namedAsKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedAsKey(ZQLParser.NamedAsKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#namedAsValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedAsValue(ZQLParser.NamedAsValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#namedAs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedAs(ZQLParser.NamedAsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery(ZQLParser.QueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#count}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCount(ZQLParser.CountContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#sumByValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSumByValue(ZQLParser.SumByValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#sumBy}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSumBy(ZQLParser.SumByContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#sum}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSum(ZQLParser.SumContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#search}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearch(ZQLParser.SearchContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyword(ZQLParser.KeywordContext ctx);
	/**
	 * Visit a parse tree produced by the {@code singleIndex}
	 * labeled alternative in {@link ZQLParser#index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleIndex(ZQLParser.SingleIndexContext ctx);
	/**
	 * Visit a parse tree produced by the {@code multiIndexs}
	 * labeled alternative in {@link ZQLParser#index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiIndexs(ZQLParser.MultiIndexsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ZQLParser#mathOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMathOperator(ZQLParser.MathOperatorContext ctx);
}
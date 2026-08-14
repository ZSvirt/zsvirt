// Generated from ZQL.g4 by ANTLR 4.7

package org.zstack.zql.antlr4;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ZQLParser}.
 */
public interface ZQLListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ZQLParser#zqls}.
	 * @param ctx the parse tree
	 */
	void enterZqls(ZQLParser.ZqlsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#zqls}.
	 * @param ctx the parse tree
	 */
	void exitZqls(ZQLParser.ZqlsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryGrammar}
	 * labeled alternative in {@link ZQLParser#zql}.
	 * @param ctx the parse tree
	 */
	void enterQueryGrammar(ZQLParser.QueryGrammarContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryGrammar}
	 * labeled alternative in {@link ZQLParser#zql}.
	 * @param ctx the parse tree
	 */
	void exitQueryGrammar(ZQLParser.QueryGrammarContext ctx);
	/**
	 * Enter a parse tree produced by the {@code countGrammar}
	 * labeled alternative in {@link ZQLParser#zql}.
	 * @param ctx the parse tree
	 */
	void enterCountGrammar(ZQLParser.CountGrammarContext ctx);
	/**
	 * Exit a parse tree produced by the {@code countGrammar}
	 * labeled alternative in {@link ZQLParser#zql}.
	 * @param ctx the parse tree
	 */
	void exitCountGrammar(ZQLParser.CountGrammarContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sumGrammar}
	 * labeled alternative in {@link ZQLParser#zql}.
	 * @param ctx the parse tree
	 */
	void enterSumGrammar(ZQLParser.SumGrammarContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sumGrammar}
	 * labeled alternative in {@link ZQLParser#zql}.
	 * @param ctx the parse tree
	 */
	void exitSumGrammar(ZQLParser.SumGrammarContext ctx);
	/**
	 * Enter a parse tree produced by the {@code searchGrammar}
	 * labeled alternative in {@link ZQLParser#zql}.
	 * @param ctx the parse tree
	 */
	void enterSearchGrammar(ZQLParser.SearchGrammarContext ctx);
	/**
	 * Exit a parse tree produced by the {@code searchGrammar}
	 * labeled alternative in {@link ZQLParser#zql}.
	 * @param ctx the parse tree
	 */
	void exitSearchGrammar(ZQLParser.SearchGrammarContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#entity}.
	 * @param ctx the parse tree
	 */
	void enterEntity(ZQLParser.EntityContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#entity}.
	 * @param ctx the parse tree
	 */
	void exitEntity(ZQLParser.EntityContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#field}.
	 * @param ctx the parse tree
	 */
	void enterField(ZQLParser.FieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#field}.
	 * @param ctx the parse tree
	 */
	void exitField(ZQLParser.FieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#multiFields}.
	 * @param ctx the parse tree
	 */
	void enterMultiFields(ZQLParser.MultiFieldsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#multiFields}.
	 * @param ctx the parse tree
	 */
	void exitMultiFields(ZQLParser.MultiFieldsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#operator}.
	 * @param ctx the parse tree
	 */
	void enterOperator(ZQLParser.OperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#operator}.
	 * @param ctx the parse tree
	 */
	void exitOperator(ZQLParser.OperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(ZQLParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(ZQLParser.ValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#listValue}.
	 * @param ctx the parse tree
	 */
	void enterListValue(ZQLParser.ListValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#listValue}.
	 * @param ctx the parse tree
	 */
	void exitListValue(ZQLParser.ListValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#logicalOperator}.
	 * @param ctx the parse tree
	 */
	void enterLogicalOperator(ZQLParser.LogicalOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#logicalOperator}.
	 * @param ctx the parse tree
	 */
	void exitLogicalOperator(ZQLParser.LogicalOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleValue}
	 * labeled alternative in {@link ZQLParser#complexValue}.
	 * @param ctx the parse tree
	 */
	void enterSimpleValue(ZQLParser.SimpleValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleValue}
	 * labeled alternative in {@link ZQLParser#complexValue}.
	 * @param ctx the parse tree
	 */
	void exitSimpleValue(ZQLParser.SimpleValueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subQueryValue}
	 * labeled alternative in {@link ZQLParser#complexValue}.
	 * @param ctx the parse tree
	 */
	void enterSubQueryValue(ZQLParser.SubQueryValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subQueryValue}
	 * labeled alternative in {@link ZQLParser#complexValue}.
	 * @param ctx the parse tree
	 */
	void exitSubQueryValue(ZQLParser.SubQueryValueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code apiGetValue}
	 * labeled alternative in {@link ZQLParser#complexValue}.
	 * @param ctx the parse tree
	 */
	void enterApiGetValue(ZQLParser.ApiGetValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code apiGetValue}
	 * labeled alternative in {@link ZQLParser#complexValue}.
	 * @param ctx the parse tree
	 */
	void exitApiGetValue(ZQLParser.ApiGetValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#getQuery}.
	 * @param ctx the parse tree
	 */
	void enterGetQuery(ZQLParser.GetQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#getQuery}.
	 * @param ctx the parse tree
	 */
	void exitGetQuery(ZQLParser.GetQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#apiparams}.
	 * @param ctx the parse tree
	 */
	void enterApiparams(ZQLParser.ApiparamsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#apiparams}.
	 * @param ctx the parse tree
	 */
	void exitApiparams(ZQLParser.ApiparamsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#input}.
	 * @param ctx the parse tree
	 */
	void enterInput(ZQLParser.InputContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#input}.
	 * @param ctx the parse tree
	 */
	void exitInput(ZQLParser.InputContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#output}.
	 * @param ctx the parse tree
	 */
	void enterOutput(ZQLParser.OutputContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#output}.
	 * @param ctx the parse tree
	 */
	void exitOutput(ZQLParser.OutputContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(ZQLParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(ZQLParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code columnNameExprAtom}
	 * labeled alternative in {@link ZQLParser#exprAtom}.
	 * @param ctx the parse tree
	 */
	void enterColumnNameExprAtom(ZQLParser.ColumnNameExprAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code columnNameExprAtom}
	 * labeled alternative in {@link ZQLParser#exprAtom}.
	 * @param ctx the parse tree
	 */
	void exitColumnNameExprAtom(ZQLParser.ColumnNameExprAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code mathExprAtom}
	 * labeled alternative in {@link ZQLParser#exprAtom}.
	 * @param ctx the parse tree
	 */
	void enterMathExprAtom(ZQLParser.MathExprAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code mathExprAtom}
	 * labeled alternative in {@link ZQLParser#exprAtom}.
	 * @param ctx the parse tree
	 */
	void exitMathExprAtom(ZQLParser.MathExprAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nestedExprAtom}
	 * labeled alternative in {@link ZQLParser#exprAtom}.
	 * @param ctx the parse tree
	 */
	void enterNestedExprAtom(ZQLParser.NestedExprAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nestedExprAtom}
	 * labeled alternative in {@link ZQLParser#exprAtom}.
	 * @param ctx the parse tree
	 */
	void exitNestedExprAtom(ZQLParser.NestedExprAtomContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#equal}.
	 * @param ctx the parse tree
	 */
	void enterEqual(ZQLParser.EqualContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#equal}.
	 * @param ctx the parse tree
	 */
	void exitEqual(ZQLParser.EqualContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nestCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterNestCondition(ZQLParser.NestConditionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nestCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitNestCondition(ZQLParser.NestConditionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterSimpleCondition(ZQLParser.SimpleConditionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitSimpleCondition(ZQLParser.SimpleConditionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesisCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterParenthesisCondition(ZQLParser.ParenthesisConditionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesisCondition}
	 * labeled alternative in {@link ZQLParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitParenthesisCondition(ZQLParser.ParenthesisConditionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code onlyEntity}
	 * labeled alternative in {@link ZQLParser#queryTarget}.
	 * @param ctx the parse tree
	 */
	void enterOnlyEntity(ZQLParser.OnlyEntityContext ctx);
	/**
	 * Exit a parse tree produced by the {@code onlyEntity}
	 * labeled alternative in {@link ZQLParser#queryTarget}.
	 * @param ctx the parse tree
	 */
	void exitOnlyEntity(ZQLParser.OnlyEntityContext ctx);
	/**
	 * Enter a parse tree produced by the {@code withSingleField}
	 * labeled alternative in {@link ZQLParser#queryTarget}.
	 * @param ctx the parse tree
	 */
	void enterWithSingleField(ZQLParser.WithSingleFieldContext ctx);
	/**
	 * Exit a parse tree produced by the {@code withSingleField}
	 * labeled alternative in {@link ZQLParser#queryTarget}.
	 * @param ctx the parse tree
	 */
	void exitWithSingleField(ZQLParser.WithSingleFieldContext ctx);
	/**
	 * Enter a parse tree produced by the {@code withMultiFields}
	 * labeled alternative in {@link ZQLParser#queryTarget}.
	 * @param ctx the parse tree
	 */
	void enterWithMultiFields(ZQLParser.WithMultiFieldsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code withMultiFields}
	 * labeled alternative in {@link ZQLParser#queryTarget}.
	 * @param ctx the parse tree
	 */
	void exitWithMultiFields(ZQLParser.WithMultiFieldsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#function}.
	 * @param ctx the parse tree
	 */
	void enterFunction(ZQLParser.FunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#function}.
	 * @param ctx the parse tree
	 */
	void exitFunction(ZQLParser.FunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code withoutFunction}
	 * labeled alternative in {@link ZQLParser#queryTargetWithFunction}.
	 * @param ctx the parse tree
	 */
	void enterWithoutFunction(ZQLParser.WithoutFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code withoutFunction}
	 * labeled alternative in {@link ZQLParser#queryTargetWithFunction}.
	 * @param ctx the parse tree
	 */
	void exitWithoutFunction(ZQLParser.WithoutFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code withFunction}
	 * labeled alternative in {@link ZQLParser#queryTargetWithFunction}.
	 * @param ctx the parse tree
	 */
	void enterWithFunction(ZQLParser.WithFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code withFunction}
	 * labeled alternative in {@link ZQLParser#queryTargetWithFunction}.
	 * @param ctx the parse tree
	 */
	void exitWithFunction(ZQLParser.WithFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#orderByExpr}.
	 * @param ctx the parse tree
	 */
	void enterOrderByExpr(ZQLParser.OrderByExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#orderByExpr}.
	 * @param ctx the parse tree
	 */
	void exitOrderByExpr(ZQLParser.OrderByExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#orderBy}.
	 * @param ctx the parse tree
	 */
	void enterOrderBy(ZQLParser.OrderByContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#orderBy}.
	 * @param ctx the parse tree
	 */
	void exitOrderBy(ZQLParser.OrderByContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#limit}.
	 * @param ctx the parse tree
	 */
	void enterLimit(ZQLParser.LimitContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#limit}.
	 * @param ctx the parse tree
	 */
	void exitLimit(ZQLParser.LimitContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#offset}.
	 * @param ctx the parse tree
	 */
	void enterOffset(ZQLParser.OffsetContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#offset}.
	 * @param ctx the parse tree
	 */
	void exitOffset(ZQLParser.OffsetContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#restrictByExpr}.
	 * @param ctx the parse tree
	 */
	void enterRestrictByExpr(ZQLParser.RestrictByExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#restrictByExpr}.
	 * @param ctx the parse tree
	 */
	void exitRestrictByExpr(ZQLParser.RestrictByExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#restrictBy}.
	 * @param ctx the parse tree
	 */
	void enterRestrictBy(ZQLParser.RestrictByContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#restrictBy}.
	 * @param ctx the parse tree
	 */
	void exitRestrictBy(ZQLParser.RestrictByContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#returnWithExprBlock}.
	 * @param ctx the parse tree
	 */
	void enterReturnWithExprBlock(ZQLParser.ReturnWithExprBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#returnWithExprBlock}.
	 * @param ctx the parse tree
	 */
	void exitReturnWithExprBlock(ZQLParser.ReturnWithExprBlockContext ctx);
	/**
	 * Enter a parse tree produced by the {@code returnWithExprId}
	 * labeled alternative in {@link ZQLParser#returnWithExpr}.
	 * @param ctx the parse tree
	 */
	void enterReturnWithExprId(ZQLParser.ReturnWithExprIdContext ctx);
	/**
	 * Exit a parse tree produced by the {@code returnWithExprId}
	 * labeled alternative in {@link ZQLParser#returnWithExpr}.
	 * @param ctx the parse tree
	 */
	void exitReturnWithExprId(ZQLParser.ReturnWithExprIdContext ctx);
	/**
	 * Enter a parse tree produced by the {@code returnWithExprFunction}
	 * labeled alternative in {@link ZQLParser#returnWithExpr}.
	 * @param ctx the parse tree
	 */
	void enterReturnWithExprFunction(ZQLParser.ReturnWithExprFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code returnWithExprFunction}
	 * labeled alternative in {@link ZQLParser#returnWithExpr}.
	 * @param ctx the parse tree
	 */
	void exitReturnWithExprFunction(ZQLParser.ReturnWithExprFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#returnWith}.
	 * @param ctx the parse tree
	 */
	void enterReturnWith(ZQLParser.ReturnWithContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#returnWith}.
	 * @param ctx the parse tree
	 */
	void exitReturnWith(ZQLParser.ReturnWithContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#groupByExpr}.
	 * @param ctx the parse tree
	 */
	void enterGroupByExpr(ZQLParser.GroupByExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#groupByExpr}.
	 * @param ctx the parse tree
	 */
	void exitGroupByExpr(ZQLParser.GroupByExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#groupBy}.
	 * @param ctx the parse tree
	 */
	void enterGroupBy(ZQLParser.GroupByContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#groupBy}.
	 * @param ctx the parse tree
	 */
	void exitGroupBy(ZQLParser.GroupByContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#subQueryTarget}.
	 * @param ctx the parse tree
	 */
	void enterSubQueryTarget(ZQLParser.SubQueryTargetContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#subQueryTarget}.
	 * @param ctx the parse tree
	 */
	void exitSubQueryTarget(ZQLParser.SubQueryTargetContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#subQuery}.
	 * @param ctx the parse tree
	 */
	void enterSubQuery(ZQLParser.SubQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#subQuery}.
	 * @param ctx the parse tree
	 */
	void exitSubQuery(ZQLParser.SubQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#filterByExprBlock}.
	 * @param ctx the parse tree
	 */
	void enterFilterByExprBlock(ZQLParser.FilterByExprBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#filterByExprBlock}.
	 * @param ctx the parse tree
	 */
	void exitFilterByExprBlock(ZQLParser.FilterByExprBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#filterByExpr}.
	 * @param ctx the parse tree
	 */
	void enterFilterByExpr(ZQLParser.FilterByExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#filterByExpr}.
	 * @param ctx the parse tree
	 */
	void exitFilterByExpr(ZQLParser.FilterByExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#filterBy}.
	 * @param ctx the parse tree
	 */
	void enterFilterBy(ZQLParser.FilterByContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#filterBy}.
	 * @param ctx the parse tree
	 */
	void exitFilterBy(ZQLParser.FilterByContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#namedAsKey}.
	 * @param ctx the parse tree
	 */
	void enterNamedAsKey(ZQLParser.NamedAsKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#namedAsKey}.
	 * @param ctx the parse tree
	 */
	void exitNamedAsKey(ZQLParser.NamedAsKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#namedAsValue}.
	 * @param ctx the parse tree
	 */
	void enterNamedAsValue(ZQLParser.NamedAsValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#namedAsValue}.
	 * @param ctx the parse tree
	 */
	void exitNamedAsValue(ZQLParser.NamedAsValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#namedAs}.
	 * @param ctx the parse tree
	 */
	void enterNamedAs(ZQLParser.NamedAsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#namedAs}.
	 * @param ctx the parse tree
	 */
	void exitNamedAs(ZQLParser.NamedAsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#query}.
	 * @param ctx the parse tree
	 */
	void enterQuery(ZQLParser.QueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#query}.
	 * @param ctx the parse tree
	 */
	void exitQuery(ZQLParser.QueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#count}.
	 * @param ctx the parse tree
	 */
	void enterCount(ZQLParser.CountContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#count}.
	 * @param ctx the parse tree
	 */
	void exitCount(ZQLParser.CountContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#sumByValue}.
	 * @param ctx the parse tree
	 */
	void enterSumByValue(ZQLParser.SumByValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#sumByValue}.
	 * @param ctx the parse tree
	 */
	void exitSumByValue(ZQLParser.SumByValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#sumBy}.
	 * @param ctx the parse tree
	 */
	void enterSumBy(ZQLParser.SumByContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#sumBy}.
	 * @param ctx the parse tree
	 */
	void exitSumBy(ZQLParser.SumByContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#sum}.
	 * @param ctx the parse tree
	 */
	void enterSum(ZQLParser.SumContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#sum}.
	 * @param ctx the parse tree
	 */
	void exitSum(ZQLParser.SumContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#search}.
	 * @param ctx the parse tree
	 */
	void enterSearch(ZQLParser.SearchContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#search}.
	 * @param ctx the parse tree
	 */
	void exitSearch(ZQLParser.SearchContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#keyword}.
	 * @param ctx the parse tree
	 */
	void enterKeyword(ZQLParser.KeywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#keyword}.
	 * @param ctx the parse tree
	 */
	void exitKeyword(ZQLParser.KeywordContext ctx);
	/**
	 * Enter a parse tree produced by the {@code singleIndex}
	 * labeled alternative in {@link ZQLParser#index}.
	 * @param ctx the parse tree
	 */
	void enterSingleIndex(ZQLParser.SingleIndexContext ctx);
	/**
	 * Exit a parse tree produced by the {@code singleIndex}
	 * labeled alternative in {@link ZQLParser#index}.
	 * @param ctx the parse tree
	 */
	void exitSingleIndex(ZQLParser.SingleIndexContext ctx);
	/**
	 * Enter a parse tree produced by the {@code multiIndexs}
	 * labeled alternative in {@link ZQLParser#index}.
	 * @param ctx the parse tree
	 */
	void enterMultiIndexs(ZQLParser.MultiIndexsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code multiIndexs}
	 * labeled alternative in {@link ZQLParser#index}.
	 * @param ctx the parse tree
	 */
	void exitMultiIndexs(ZQLParser.MultiIndexsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZQLParser#mathOperator}.
	 * @param ctx the parse tree
	 */
	void enterMathOperator(ZQLParser.MathOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZQLParser#mathOperator}.
	 * @param ctx the parse tree
	 */
	void exitMathOperator(ZQLParser.MathOperatorContext ctx);
}
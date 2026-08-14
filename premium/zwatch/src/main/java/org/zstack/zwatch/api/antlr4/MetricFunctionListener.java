// Generated from MetricFunction.g4 by ANTLR 4.7

   package org.zstack.zwatch.api.antlr4;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link MetricFunctionParser}.
 */
public interface MetricFunctionListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link MetricFunctionParser#function}.
	 * @param ctx the parse tree
	 */
	void enterFunction(MetricFunctionParser.FunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MetricFunctionParser#function}.
	 * @param ctx the parse tree
	 */
	void exitFunction(MetricFunctionParser.FunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MetricFunctionParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(MetricFunctionParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MetricFunctionParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(MetricFunctionParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MetricFunctionParser#func}.
	 * @param ctx the parse tree
	 */
	void enterFunc(MetricFunctionParser.FuncContext ctx);
	/**
	 * Exit a parse tree produced by {@link MetricFunctionParser#func}.
	 * @param ctx the parse tree
	 */
	void exitFunc(MetricFunctionParser.FuncContext ctx);
	/**
	 * Enter a parse tree produced by the {@code argInt}
	 * labeled alternative in {@link MetricFunctionParser#arguements}.
	 * @param ctx the parse tree
	 */
	void enterArgInt(MetricFunctionParser.ArgIntContext ctx);
	/**
	 * Exit a parse tree produced by the {@code argInt}
	 * labeled alternative in {@link MetricFunctionParser#arguements}.
	 * @param ctx the parse tree
	 */
	void exitArgInt(MetricFunctionParser.ArgIntContext ctx);
	/**
	 * Enter a parse tree produced by the {@code argString}
	 * labeled alternative in {@link MetricFunctionParser#arguements}.
	 * @param ctx the parse tree
	 */
	void enterArgString(MetricFunctionParser.ArgStringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code argString}
	 * labeled alternative in {@link MetricFunctionParser#arguements}.
	 * @param ctx the parse tree
	 */
	void exitArgString(MetricFunctionParser.ArgStringContext ctx);
	/**
	 * Enter a parse tree produced by the {@code argFloat}
	 * labeled alternative in {@link MetricFunctionParser#arguements}.
	 * @param ctx the parse tree
	 */
	void enterArgFloat(MetricFunctionParser.ArgFloatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code argFloat}
	 * labeled alternative in {@link MetricFunctionParser#arguements}.
	 * @param ctx the parse tree
	 */
	void exitArgFloat(MetricFunctionParser.ArgFloatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code argChar}
	 * labeled alternative in {@link MetricFunctionParser#arguements}.
	 * @param ctx the parse tree
	 */
	void enterArgChar(MetricFunctionParser.ArgCharContext ctx);
	/**
	 * Exit a parse tree produced by the {@code argChar}
	 * labeled alternative in {@link MetricFunctionParser#arguements}.
	 * @param ctx the parse tree
	 */
	void exitArgChar(MetricFunctionParser.ArgCharContext ctx);
	/**
	 * Enter a parse tree produced by {@link MetricFunctionParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void enterArgumentList(MetricFunctionParser.ArgumentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link MetricFunctionParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void exitArgumentList(MetricFunctionParser.ArgumentListContext ctx);
}
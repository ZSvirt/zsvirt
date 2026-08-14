// Generated from TriggerExpression.g4 by ANTLR 4.7

   package org.zstack.monitoring.trigger.expression.antlr4;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link TriggerExpressionParser}.
 */
public interface TriggerExpressionListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link TriggerExpressionParser#trigger}.
	 * @param ctx the parse tree
	 */
	void enterTrigger(TriggerExpressionParser.TriggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link TriggerExpressionParser#trigger}.
	 * @param ctx the parse tree
	 */
	void exitTrigger(TriggerExpressionParser.TriggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link TriggerExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(TriggerExpressionParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TriggerExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(TriggerExpressionParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TriggerExpressionParser#item}.
	 * @param ctx the parse tree
	 */
	void enterItem(TriggerExpressionParser.ItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link TriggerExpressionParser#item}.
	 * @param ctx the parse tree
	 */
	void exitItem(TriggerExpressionParser.ItemContext ctx);
	/**
	 * Enter a parse tree produced by the {@code argInt}
	 * labeled alternative in {@link TriggerExpressionParser#arguements}.
	 * @param ctx the parse tree
	 */
	void enterArgInt(TriggerExpressionParser.ArgIntContext ctx);
	/**
	 * Exit a parse tree produced by the {@code argInt}
	 * labeled alternative in {@link TriggerExpressionParser#arguements}.
	 * @param ctx the parse tree
	 */
	void exitArgInt(TriggerExpressionParser.ArgIntContext ctx);
	/**
	 * Enter a parse tree produced by the {@code argString}
	 * labeled alternative in {@link TriggerExpressionParser#arguements}.
	 * @param ctx the parse tree
	 */
	void enterArgString(TriggerExpressionParser.ArgStringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code argString}
	 * labeled alternative in {@link TriggerExpressionParser#arguements}.
	 * @param ctx the parse tree
	 */
	void exitArgString(TriggerExpressionParser.ArgStringContext ctx);
	/**
	 * Enter a parse tree produced by {@link TriggerExpressionParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void enterArgumentList(TriggerExpressionParser.ArgumentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link TriggerExpressionParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void exitArgumentList(TriggerExpressionParser.ArgumentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link TriggerExpressionParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterConstant(TriggerExpressionParser.ConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link TriggerExpressionParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitConstant(TriggerExpressionParser.ConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link TriggerExpressionParser#operator}.
	 * @param ctx the parse tree
	 */
	void enterOperator(TriggerExpressionParser.OperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link TriggerExpressionParser#operator}.
	 * @param ctx the parse tree
	 */
	void exitOperator(TriggerExpressionParser.OperatorContext ctx);
}
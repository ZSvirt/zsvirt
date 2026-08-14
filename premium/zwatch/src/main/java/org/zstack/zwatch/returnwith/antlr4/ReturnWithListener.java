// Generated from ReturnWith.g4 by ANTLR 4.7

   package org.zstack.zwatch.returnwith.antlr4;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ReturnWithParser}.
 */
public interface ReturnWithListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ReturnWithParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(ReturnWithParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReturnWithParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(ReturnWithParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReturnWithParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(ReturnWithParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReturnWithParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(ReturnWithParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code valueStr}
	 * labeled alternative in {@link ReturnWithParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValueStr(ReturnWithParser.ValueStrContext ctx);
	/**
	 * Exit a parse tree produced by the {@code valueStr}
	 * labeled alternative in {@link ReturnWithParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValueStr(ReturnWithParser.ValueStrContext ctx);
	/**
	 * Enter a parse tree produced by the {@code valueNum}
	 * labeled alternative in {@link ReturnWithParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValueNum(ReturnWithParser.ValueNumContext ctx);
	/**
	 * Exit a parse tree produced by the {@code valueNum}
	 * labeled alternative in {@link ReturnWithParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValueNum(ReturnWithParser.ValueNumContext ctx);
	/**
	 * Enter a parse tree produced by the {@code valueFunc}
	 * labeled alternative in {@link ReturnWithParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValueFunc(ReturnWithParser.ValueFuncContext ctx);
	/**
	 * Exit a parse tree produced by the {@code valueFunc}
	 * labeled alternative in {@link ReturnWithParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValueFunc(ReturnWithParser.ValueFuncContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReturnWithParser#func}.
	 * @param ctx the parse tree
	 */
	void enterFunc(ReturnWithParser.FuncContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReturnWithParser#func}.
	 * @param ctx the parse tree
	 */
	void exitFunc(ReturnWithParser.FuncContext ctx);
}
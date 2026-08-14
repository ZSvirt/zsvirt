// Generated from ReturnWith.g4 by ANTLR 4.7

   package org.zstack.zwatch.returnwith.antlr4;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ReturnWithParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ReturnWithVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ReturnWithParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(ReturnWithParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ReturnWithParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(ReturnWithParser.ExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code valueStr}
	 * labeled alternative in {@link ReturnWithParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueStr(ReturnWithParser.ValueStrContext ctx);
	/**
	 * Visit a parse tree produced by the {@code valueNum}
	 * labeled alternative in {@link ReturnWithParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueNum(ReturnWithParser.ValueNumContext ctx);
	/**
	 * Visit a parse tree produced by the {@code valueFunc}
	 * labeled alternative in {@link ReturnWithParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueFunc(ReturnWithParser.ValueFuncContext ctx);
	/**
	 * Visit a parse tree produced by {@link ReturnWithParser#func}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc(ReturnWithParser.FuncContext ctx);
}
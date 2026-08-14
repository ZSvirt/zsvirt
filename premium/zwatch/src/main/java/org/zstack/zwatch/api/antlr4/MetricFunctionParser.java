// Generated from MetricFunction.g4 by ANTLR 4.7

   package org.zstack.zwatch.api.antlr4;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class MetricFunctionParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, ID=5, INT=6, FLOAT=7, WS=8, STRING=9, 
		VALUE=10;
	public static final int
		RULE_function = 0, RULE_expression = 1, RULE_func = 2, RULE_arguements = 3, 
		RULE_argumentList = 4;
	public static final String[] ruleNames = {
		"function", "expression", "func", "arguements", "argumentList"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'('", "')'", "'='", "','"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, "ID", "INT", "FLOAT", "WS", "STRING", "VALUE"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "MetricFunction.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public MetricFunctionParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class FunctionContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public FunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_function; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MetricFunctionListener ) ((MetricFunctionListener)listener).enterFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MetricFunctionListener ) ((MetricFunctionListener)listener).exitFunction(this);
		}
	}

	public final FunctionContext function() throws RecognitionException {
		FunctionContext _localctx = new FunctionContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_function);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(10);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionContext extends ParserRuleContext {
		public FuncContext func() {
			return getRuleContext(FuncContext.class,0);
		}
		public ArgumentListContext argumentList() {
			return getRuleContext(ArgumentListContext.class,0);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MetricFunctionListener ) ((MetricFunctionListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MetricFunctionListener ) ((MetricFunctionListener)listener).exitExpression(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(12);
			func();
			setState(13);
			match(T__0);
			setState(14);
			argumentList();
			setState(15);
			match(T__1);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FuncContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(MetricFunctionParser.ID, 0); }
		public FuncContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_func; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MetricFunctionListener ) ((MetricFunctionListener)listener).enterFunc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MetricFunctionListener ) ((MetricFunctionListener)listener).exitFunc(this);
		}
	}

	public final FuncContext func() throws RecognitionException {
		FuncContext _localctx = new FuncContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_func);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(17);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArguementsContext extends ParserRuleContext {
		public ArguementsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arguements; }
	 
		public ArguementsContext() { }
		public void copyFrom(ArguementsContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ArgIntContext extends ArguementsContext {
		public TerminalNode ID() { return getToken(MetricFunctionParser.ID, 0); }
		public TerminalNode INT() { return getToken(MetricFunctionParser.INT, 0); }
		public ArgIntContext(ArguementsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MetricFunctionListener ) ((MetricFunctionListener)listener).enterArgInt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MetricFunctionListener ) ((MetricFunctionListener)listener).exitArgInt(this);
		}
	}
	public static class ArgStringContext extends ArguementsContext {
		public TerminalNode ID() { return getToken(MetricFunctionParser.ID, 0); }
		public TerminalNode STRING() { return getToken(MetricFunctionParser.STRING, 0); }
		public ArgStringContext(ArguementsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MetricFunctionListener ) ((MetricFunctionListener)listener).enterArgString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MetricFunctionListener ) ((MetricFunctionListener)listener).exitArgString(this);
		}
	}
	public static class ArgCharContext extends ArguementsContext {
		public TerminalNode ID() { return getToken(MetricFunctionParser.ID, 0); }
		public TerminalNode VALUE() { return getToken(MetricFunctionParser.VALUE, 0); }
		public ArgCharContext(ArguementsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MetricFunctionListener ) ((MetricFunctionListener)listener).enterArgChar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MetricFunctionListener ) ((MetricFunctionListener)listener).exitArgChar(this);
		}
	}
	public static class ArgFloatContext extends ArguementsContext {
		public TerminalNode ID() { return getToken(MetricFunctionParser.ID, 0); }
		public TerminalNode FLOAT() { return getToken(MetricFunctionParser.FLOAT, 0); }
		public ArgFloatContext(ArguementsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MetricFunctionListener ) ((MetricFunctionListener)listener).enterArgFloat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MetricFunctionListener ) ((MetricFunctionListener)listener).exitArgFloat(this);
		}
	}

	public final ArguementsContext arguements() throws RecognitionException {
		ArguementsContext _localctx = new ArguementsContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_arguements);
		try {
			setState(31);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				_localctx = new ArgIntContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(19);
				match(ID);
				setState(20);
				match(T__2);
				setState(21);
				match(INT);
				}
				break;
			case 2:
				_localctx = new ArgStringContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(22);
				match(ID);
				setState(23);
				match(T__2);
				setState(24);
				match(STRING);
				}
				break;
			case 3:
				_localctx = new ArgFloatContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(25);
				match(ID);
				setState(26);
				match(T__2);
				setState(27);
				match(FLOAT);
				}
				break;
			case 4:
				_localctx = new ArgCharContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(28);
				match(ID);
				setState(29);
				match(T__2);
				setState(30);
				match(VALUE);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArgumentListContext extends ParserRuleContext {
		public List<ArguementsContext> arguements() {
			return getRuleContexts(ArguementsContext.class);
		}
		public ArguementsContext arguements(int i) {
			return getRuleContext(ArguementsContext.class,i);
		}
		public ArgumentListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argumentList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MetricFunctionListener ) ((MetricFunctionListener)listener).enterArgumentList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MetricFunctionListener ) ((MetricFunctionListener)listener).exitArgumentList(this);
		}
	}

	public final ArgumentListContext argumentList() throws RecognitionException {
		ArgumentListContext _localctx = new ArgumentListContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_argumentList);
		int _la;
		try {
			setState(42);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(33);
				arguements();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(34);
				arguements();
				setState(37); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(35);
					match(T__3);
					setState(36);
					arguements();
					}
					}
					setState(39); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__3 );
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\f/\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\5\3\5"+
		"\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\5\5\"\n\5\3\6\3\6\3\6\3\6\6\6"+
		"(\n\6\r\6\16\6)\3\6\5\6-\n\6\3\6\2\2\7\2\4\6\b\n\2\2\2/\2\f\3\2\2\2\4"+
		"\16\3\2\2\2\6\23\3\2\2\2\b!\3\2\2\2\n,\3\2\2\2\f\r\5\4\3\2\r\3\3\2\2\2"+
		"\16\17\5\6\4\2\17\20\7\3\2\2\20\21\5\n\6\2\21\22\7\4\2\2\22\5\3\2\2\2"+
		"\23\24\7\7\2\2\24\7\3\2\2\2\25\26\7\7\2\2\26\27\7\5\2\2\27\"\7\b\2\2\30"+
		"\31\7\7\2\2\31\32\7\5\2\2\32\"\7\13\2\2\33\34\7\7\2\2\34\35\7\5\2\2\35"+
		"\"\7\t\2\2\36\37\7\7\2\2\37 \7\5\2\2 \"\7\f\2\2!\25\3\2\2\2!\30\3\2\2"+
		"\2!\33\3\2\2\2!\36\3\2\2\2\"\t\3\2\2\2#-\5\b\5\2$\'\5\b\5\2%&\7\6\2\2"+
		"&(\5\b\5\2\'%\3\2\2\2()\3\2\2\2)\'\3\2\2\2)*\3\2\2\2*-\3\2\2\2+-\3\2\2"+
		"\2,#\3\2\2\2,$\3\2\2\2,+\3\2\2\2-\13\3\2\2\2\5!),";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
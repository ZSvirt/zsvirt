// Generated from ReturnWith.g4 by ANTLR 4.7

   package org.zstack.zwatch.returnwith.antlr4;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ReturnWithLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, ID=5, INT=6, FLOAT=7, WS=8, STRING=9, 
		VALUE=10;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "ID", "INT", "FLOAT", "WS", "STRING", 
		"VALUE", "CHAR", "NUMBER"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "','", "'='", "'('", "')'"
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


	public ReturnWithLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "ReturnWith.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\fd\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\6\6&\n\6\r"+
		"\6\16\6\'\3\7\5\7+\n\7\3\7\3\7\3\b\3\b\3\b\6\b\62\n\b\r\b\16\b\63\3\t"+
		"\6\t\67\n\t\r\t\16\t8\3\t\3\t\3\n\3\n\7\n?\n\n\f\n\16\nB\13\n\3\n\3\n"+
		"\3\n\7\nG\n\n\f\n\16\nJ\13\n\3\n\5\nM\n\n\3\13\6\13P\n\13\r\13\16\13Q"+
		"\3\f\6\fU\n\f\r\f\16\fV\3\f\6\fZ\n\f\r\f\16\f[\5\f^\n\f\3\r\6\ra\n\r\r"+
		"\r\16\rb\2\2\16\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\2\31"+
		"\2\3\2\5\5\2\13\f\17\17\"\"\3\2$$\3\2))\2n\2\3\3\2\2\2\2\5\3\2\2\2\2\7"+
		"\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2"+
		"\2\2\23\3\2\2\2\2\25\3\2\2\2\3\33\3\2\2\2\5\35\3\2\2\2\7\37\3\2\2\2\t"+
		"!\3\2\2\2\13%\3\2\2\2\r*\3\2\2\2\17.\3\2\2\2\21\66\3\2\2\2\23L\3\2\2\2"+
		"\25O\3\2\2\2\27]\3\2\2\2\31`\3\2\2\2\33\34\7.\2\2\34\4\3\2\2\2\35\36\7"+
		"?\2\2\36\6\3\2\2\2\37 \7*\2\2 \b\3\2\2\2!\"\7+\2\2\"\n\3\2\2\2#&\5\27"+
		"\f\2$&\7a\2\2%#\3\2\2\2%$\3\2\2\2&\'\3\2\2\2\'%\3\2\2\2\'(\3\2\2\2(\f"+
		"\3\2\2\2)+\7/\2\2*)\3\2\2\2*+\3\2\2\2+,\3\2\2\2,-\5\31\r\2-\16\3\2\2\2"+
		"./\5\r\7\2/\61\7\60\2\2\60\62\5\31\r\2\61\60\3\2\2\2\62\63\3\2\2\2\63"+
		"\61\3\2\2\2\63\64\3\2\2\2\64\20\3\2\2\2\65\67\t\2\2\2\66\65\3\2\2\2\67"+
		"8\3\2\2\28\66\3\2\2\289\3\2\2\29:\3\2\2\2:;\b\t\2\2;\22\3\2\2\2<@\7$\2"+
		"\2=?\n\3\2\2>=\3\2\2\2?B\3\2\2\2@>\3\2\2\2@A\3\2\2\2AC\3\2\2\2B@\3\2\2"+
		"\2CM\7$\2\2DH\7)\2\2EG\n\4\2\2FE\3\2\2\2GJ\3\2\2\2HF\3\2\2\2HI\3\2\2\2"+
		"IK\3\2\2\2JH\3\2\2\2KM\7)\2\2L<\3\2\2\2LD\3\2\2\2M\24\3\2\2\2NP\5\27\f"+
		"\2ON\3\2\2\2PQ\3\2\2\2QO\3\2\2\2QR\3\2\2\2R\26\3\2\2\2SU\4c|\2TS\3\2\2"+
		"\2UV\3\2\2\2VT\3\2\2\2VW\3\2\2\2W^\3\2\2\2XZ\4C\\\2YX\3\2\2\2Z[\3\2\2"+
		"\2[Y\3\2\2\2[\\\3\2\2\2\\^\3\2\2\2]T\3\2\2\2]Y\3\2\2\2^\30\3\2\2\2_a\4"+
		"\62;\2`_\3\2\2\2ab\3\2\2\2b`\3\2\2\2bc\3\2\2\2c\32\3\2\2\2\20\2%\'*\63"+
		"8@HLQV[]b\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
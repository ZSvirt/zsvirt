// Generated from MetricFunction.g4 by ANTLR 4.7

   package org.zstack.zwatch.api.antlr4;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class MetricFunctionLexer extends Lexer {
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


	public MetricFunctionLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "MetricFunction.g4"; }

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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\ff\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\6\6&\n\6\r"+
		"\6\16\6\'\3\7\5\7+\n\7\3\7\3\7\3\b\3\b\3\b\6\b\62\n\b\r\b\16\b\63\3\t"+
		"\6\t\67\n\t\r\t\16\t8\3\t\3\t\3\n\3\n\6\n?\n\n\r\n\16\n@\3\n\3\n\6\nE"+
		"\n\n\r\n\16\nF\3\n\6\nJ\n\n\r\n\16\nK\3\n\5\nO\n\n\3\13\6\13R\n\13\r\13"+
		"\16\13S\3\f\6\fW\n\f\r\f\16\fX\3\f\6\f\\\n\f\r\f\16\f]\5\f`\n\f\3\r\6"+
		"\rc\n\r\r\r\16\rd\2\2\16\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f"+
		"\27\2\31\2\3\2\5\5\2\13\f\17\17\"\"\3\2$$\3\2))\2q\2\3\3\2\2\2\2\5\3\2"+
		"\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21"+
		"\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\3\33\3\2\2\2\5\35\3\2\2\2\7\37\3\2"+
		"\2\2\t!\3\2\2\2\13%\3\2\2\2\r*\3\2\2\2\17.\3\2\2\2\21\66\3\2\2\2\23N\3"+
		"\2\2\2\25Q\3\2\2\2\27_\3\2\2\2\31b\3\2\2\2\33\34\7*\2\2\34\4\3\2\2\2\35"+
		"\36\7+\2\2\36\6\3\2\2\2\37 \7?\2\2 \b\3\2\2\2!\"\7.\2\2\"\n\3\2\2\2#&"+
		"\5\27\f\2$&\7a\2\2%#\3\2\2\2%$\3\2\2\2&\'\3\2\2\2\'%\3\2\2\2\'(\3\2\2"+
		"\2(\f\3\2\2\2)+\7/\2\2*)\3\2\2\2*+\3\2\2\2+,\3\2\2\2,-\5\31\r\2-\16\3"+
		"\2\2\2./\5\r\7\2/\61\7\60\2\2\60\62\5\31\r\2\61\60\3\2\2\2\62\63\3\2\2"+
		"\2\63\61\3\2\2\2\63\64\3\2\2\2\64\20\3\2\2\2\65\67\t\2\2\2\66\65\3\2\2"+
		"\2\678\3\2\2\28\66\3\2\2\289\3\2\2\29:\3\2\2\2:;\b\t\2\2;\22\3\2\2\2<"+
		">\7$\2\2=?\n\3\2\2>=\3\2\2\2?@\3\2\2\2@>\3\2\2\2@A\3\2\2\2AB\3\2\2\2B"+
		"O\7$\2\2CE\7)\2\2DC\3\2\2\2EF\3\2\2\2FD\3\2\2\2FG\3\2\2\2GI\3\2\2\2HJ"+
		"\n\4\2\2IH\3\2\2\2JK\3\2\2\2KI\3\2\2\2KL\3\2\2\2LM\3\2\2\2MO\7)\2\2N<"+
		"\3\2\2\2ND\3\2\2\2O\24\3\2\2\2PR\5\27\f\2QP\3\2\2\2RS\3\2\2\2SQ\3\2\2"+
		"\2ST\3\2\2\2T\26\3\2\2\2UW\4c|\2VU\3\2\2\2WX\3\2\2\2XV\3\2\2\2XY\3\2\2"+
		"\2Y`\3\2\2\2Z\\\4C\\\2[Z\3\2\2\2\\]\3\2\2\2][\3\2\2\2]^\3\2\2\2^`\3\2"+
		"\2\2_V\3\2\2\2_[\3\2\2\2`\30\3\2\2\2ac\4\62;\2ba\3\2\2\2cd\3\2\2\2db\3"+
		"\2\2\2de\3\2\2\2e\32\3\2\2\2\21\2%\'*\638@FKNSX]_d\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
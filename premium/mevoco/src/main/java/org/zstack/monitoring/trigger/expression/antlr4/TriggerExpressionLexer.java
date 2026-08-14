// Generated from TriggerExpression.g4 by ANTLR 4.7

   package org.zstack.monitoring.trigger.expression.antlr4;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class TriggerExpressionLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, ID=14, INT=15, UNIT=16, FLOAT=17, 
		WS=18, STRING=19;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
		"T__9", "T__10", "T__11", "T__12", "ID", "INT", "UNIT", "FLOAT", "WS", 
		"STRING", "CHAR", "NUMBER"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'{'", "'}'", "'='", "','", "'-'", "'*'", "'/'", "'+'", "'<'", "'<='", 
		"'>'", "'>='", "'!='"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, "ID", "INT", "UNIT", "FLOAT", "WS", "STRING"
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


	public TriggerExpressionLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "TriggerExpression.g4"; }

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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\25\u008b\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\3\2\3\2\3\3\3\3\3\4\3\4"+
		"\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\13\3\f\3"+
		"\f\3\r\3\r\3\r\3\16\3\16\3\16\3\17\3\17\6\17M\n\17\r\17\16\17N\3\20\5"+
		"\20R\n\20\3\20\3\20\3\21\3\21\3\21\3\22\3\22\3\22\6\22\\\n\22\r\22\16"+
		"\22]\3\23\6\23a\n\23\r\23\16\23b\3\23\3\23\3\24\3\24\6\24i\n\24\r\24\16"+
		"\24j\3\24\3\24\6\24o\n\24\r\24\16\24p\3\24\6\24t\n\24\r\24\16\24u\3\24"+
		"\5\24y\n\24\3\25\6\25|\n\25\r\25\16\25}\3\25\6\25\u0081\n\25\r\25\16\25"+
		"\u0082\5\25\u0085\n\25\3\26\6\26\u0088\n\26\r\26\16\26\u0089\2\2\27\3"+
		"\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37"+
		"\21!\22#\23%\24\'\25)\2+\2\3\2\7\5\2/\60<<aa\16\2GGIIMMOORRVV[\\ffjjo"+
		"ouuyy\5\2\13\f\17\17\"\"\3\2$$\3\2))\2\u0095\2\3\3\2\2\2\2\5\3\2\2\2\2"+
		"\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2"+
		"\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2"+
		"\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2"+
		"\2\3-\3\2\2\2\5/\3\2\2\2\7\61\3\2\2\2\t\63\3\2\2\2\13\65\3\2\2\2\r\67"+
		"\3\2\2\2\179\3\2\2\2\21;\3\2\2\2\23=\3\2\2\2\25?\3\2\2\2\27B\3\2\2\2\31"+
		"D\3\2\2\2\33G\3\2\2\2\35L\3\2\2\2\37Q\3\2\2\2!U\3\2\2\2#X\3\2\2\2%`\3"+
		"\2\2\2\'x\3\2\2\2)\u0084\3\2\2\2+\u0087\3\2\2\2-.\7}\2\2.\4\3\2\2\2/\60"+
		"\7\177\2\2\60\6\3\2\2\2\61\62\7?\2\2\62\b\3\2\2\2\63\64\7.\2\2\64\n\3"+
		"\2\2\2\65\66\7/\2\2\66\f\3\2\2\2\678\7,\2\28\16\3\2\2\29:\7\61\2\2:\20"+
		"\3\2\2\2;<\7-\2\2<\22\3\2\2\2=>\7>\2\2>\24\3\2\2\2?@\7>\2\2@A\7?\2\2A"+
		"\26\3\2\2\2BC\7@\2\2C\30\3\2\2\2DE\7@\2\2EF\7?\2\2F\32\3\2\2\2GH\7#\2"+
		"\2HI\7?\2\2I\34\3\2\2\2JM\5)\25\2KM\t\2\2\2LJ\3\2\2\2LK\3\2\2\2MN\3\2"+
		"\2\2NL\3\2\2\2NO\3\2\2\2O\36\3\2\2\2PR\7/\2\2QP\3\2\2\2QR\3\2\2\2RS\3"+
		"\2\2\2ST\5+\26\2T \3\2\2\2UV\5+\26\2VW\t\3\2\2W\"\3\2\2\2XY\5\37\20\2"+
		"Y[\7\60\2\2Z\\\5+\26\2[Z\3\2\2\2\\]\3\2\2\2][\3\2\2\2]^\3\2\2\2^$\3\2"+
		"\2\2_a\t\4\2\2`_\3\2\2\2ab\3\2\2\2b`\3\2\2\2bc\3\2\2\2cd\3\2\2\2de\b\23"+
		"\2\2e&\3\2\2\2fh\7$\2\2gi\n\5\2\2hg\3\2\2\2ij\3\2\2\2jh\3\2\2\2jk\3\2"+
		"\2\2kl\3\2\2\2ly\7$\2\2mo\7)\2\2nm\3\2\2\2op\3\2\2\2pn\3\2\2\2pq\3\2\2"+
		"\2qs\3\2\2\2rt\n\6\2\2sr\3\2\2\2tu\3\2\2\2us\3\2\2\2uv\3\2\2\2vw\3\2\2"+
		"\2wy\7)\2\2xf\3\2\2\2xn\3\2\2\2y(\3\2\2\2z|\4c|\2{z\3\2\2\2|}\3\2\2\2"+
		"}{\3\2\2\2}~\3\2\2\2~\u0085\3\2\2\2\177\u0081\4C\\\2\u0080\177\3\2\2\2"+
		"\u0081\u0082\3\2\2\2\u0082\u0080\3\2\2\2\u0082\u0083\3\2\2\2\u0083\u0085"+
		"\3\2\2\2\u0084{\3\2\2\2\u0084\u0080\3\2\2\2\u0085*\3\2\2\2\u0086\u0088"+
		"\4\62;\2\u0087\u0086\3\2\2\2\u0088\u0089\3\2\2\2\u0089\u0087\3\2\2\2\u0089"+
		"\u008a\3\2\2\2\u008a,\3\2\2\2\20\2LNQ]bjpux}\u0082\u0084\u0089\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
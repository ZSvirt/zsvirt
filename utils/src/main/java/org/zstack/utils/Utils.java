package org.zstack.utils;

import org.zstack.utils.data.ArraySpliter;
import org.zstack.utils.data.ArraySpliterImpl;
import org.zstack.utils.data.FieldPrinter;
import org.zstack.utils.data.FieldPrinterImpl;
import org.zstack.utils.filelocater.FileLocator;
import org.zstack.utils.filelocater.FileLocatorImpl;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;
import org.zstack.utils.path.PathUtilImpl;
import org.zstack.utils.path.PathUtils;
import org.zstack.utils.stopwatch.StopWatch;
import org.zstack.utils.stopwatch.StopWatchImpl;
import org.zstack.utils.tester.ZTester;
import org.zstack.utils.tester.ZTesterImpl;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

public class Utils {
	private static ArraySpliter arraySpliter = null;
	private static FieldPrinter fieldPrinter = null;
	private static PathUtils pathUtil = null;

    private static ThreadLocal<Map<String, String>> maskWords = ThreadLocal.withInitial(Collections::emptyMap);

    private static Pattern simpleWordsPattern = Pattern.compile("^[a-zA-Z0-9]*$");
    private static final Function<String, String> defaultRewriter = raw -> {
        for (Map.Entry<String, String> s : maskWords.get().entrySet()) {
            if (simpleWordsPattern.matcher(s.getKey()).matches()) {
                raw = raw.replaceAll('"' + s.getKey() + "\"(?=[^:])", '"' + s.getValue() + '"')
                        .replaceAll('\'' + s.getKey() + "'(?=[^:])", '\'' + s.getValue() + '\'');
            } else {
                raw = raw.replace('"' + s.getKey() + '"', '"' + s.getValue() + '"')
                        .replace('\'' + s.getKey() + '\'', '\'' + s.getValue() + '\'');
            }
        }
        return raw;
    };

    public static String maskSensitiveInfo(String raw) {
        return defaultRewriter.apply(raw);
    }

    public static Map<String, String> getLogMaskWords() {
        return maskWords.get();
    }

    public static void registerSafeLogger(Class clazz) {
        MaskSensitiveInfoRewritePolicy.loggerNames.add(clazz.getName());
    }

    public static class MaskWords implements AutoCloseable {
        Map<String, String> originMaskWords = new HashMap<>();

        public MaskWords(Map<String, String> words) {
            this(words, false);
        }

        public MaskWords(Map<String, String> words, boolean extend) {
            if (!extend) {
                maskWords.set(new HashMap<>(words));
                return;
            }

            originMaskWords.putAll(maskWords.get());
            maskWords.set(new HashMap<>(words));
            maskWords.get().putAll(originMaskWords);
        }

        @Override
        public void close() {
            if (!originMaskWords.isEmpty()) {
                maskWords.set(originMaskWords);
            } else {
                maskWords.remove();
            }
        }
    }

	static {
		arraySpliter = new ArraySpliterImpl();
		fieldPrinter = new FieldPrinterImpl();
		pathUtil = new PathUtilImpl();
	}

    public static StopWatch getStopWatch() {
        return new StopWatchImpl();
    }

    public static CLogger getLogger(String className) {
        return CLoggerImpl.getLogger(className);
     }

    public static CLogger getLogger(Class<?> clazz) {
        return CLoggerImpl.getLogger(clazz);
    }

    public static CLogger getLogger(String className, Function<String, String> rewriter) {
        return CLoggerImpl.getLogger(className,rewriter);
    }

    public static CLogger getLogger(Class<?> clazz, Function<String, String> rewriter) {
        return CLoggerImpl.getLogger(clazz, rewriter);
    }

    public static CLogger getSafeLogger(String className) {
        return CLoggerImpl.getLogger(className, defaultRewriter);
    }

    public static CLogger getSafeLogger(Class<?> clazz) {
        return CLoggerImpl.getLogger(clazz, defaultRewriter);
    }

    public static FileLocator createFileLocator() {
        return new FileLocatorImpl();
    }

    public static ArraySpliter getArraySpliter() {
    	return arraySpliter;
    }

    public static FieldPrinter getFieldPrinter() {
    	return fieldPrinter;
    }

    public static PathUtils getPathUtil() {
    	return pathUtil;
    }

    public static ZTester getTester() {
        return ZTesterImpl.getTester();
    }
}

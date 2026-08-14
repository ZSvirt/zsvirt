package org.zstack.utils.string;

import com.github.sisyphsu.retree.ReMatcher;
import com.github.sisyphsu.retree.ReTree;
import info.debatty.java.stringsimilarity.*;
import org.apache.commons.io.FileUtils;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Created by mingjian.deng on 2018/11/28.
 */
public class StringSimilarity {
    private static final CLogger logger = Utils.getLogger(StringSimilarity.class);

    public static String elaborateFolder = "errorElaborations";
    public static File classPathFolder = PathUtil.findFolderOnClassPath(StringSimilarity.elaborateFolder);

    // used for Jaro Similarity
    private static final double threshold = 0.1;

    private static final int mapLength = 1500;
    public static int maxElaborationRegex = 8192;

    // matched errors
    private static final Map<String, ErrorCodeElaboration> errors = new LinkedHashMap<String, ErrorCodeElaboration>(mapLength, 0.9f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return this.size() > mapLength;
        }
    };

    // missed errors
    private static final Map<String, Boolean> missed = new LinkedHashMap<String, Boolean>(mapLength, 0.9f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return this.size() > mapLength;
        }
    };

    private static final Map<String, Pattern> patterns = new ConcurrentHashMap<>();

    private static ReTree retrees;

    // initial errors from json files
    private static List<ErrorCodeElaboration> elaborations = initialElaborations();

    /**
     * check if the elaboration matches
     * 1. check if the error is matched by regex
     * 2. check if the error's distance is less than threshold
     *
     * @param err error code elaboration
     * @return true if the error is matched
     */
    public static boolean matched(ErrorCodeElaboration err) {
        if (err == null) {
            return false;
        }
        if (ElaborationSearchMethod.regex == err.getMethod()) {
            return true;
        }

        return err.getDistance() < threshold;
    }

    public static List<ErrorCodeElaboration> getElaborations() {
        return new ArrayList<>(elaborations);
    }

    /**
     * reset the cached errors and missed errors
     */
    public static void resetCachedErrors() {
        synchronized(errors) {
            errors.clear();
        }
        synchronized(missed) {
            missed.clear();
        }
    }

    /**
     * add the error code elaboration to the errors cache.
     *
     * @param key error code fmt string
     * @param err error code elaboration
     */
    public static void addErrors(String key, ErrorCodeElaboration err) {
        if (err != null) {
            synchronized(errors) {
                errors.put(key, err);
            }
        }
    }

    /**
     * add the error code fmt string to the missed cache.
     *
     * @param key error code fmt string
     */
    public static void addMissed(String key) {
        synchronized(missed){
            missed.put(key, true);
        }
    }

    /**
     * refresh the error code elaborations from the json files.
     */
    public static void refreshErrorTemplates() {
        elaborations = initialElaborations();
    }

    private static void validate(List<ErrorCodeElaboration> els) {
        for (ErrorCodeElaboration e: els) {
            DebugUtils.Assert(e.getCategory() != null, String.format("category is null for elaboration: %s", e.getRegex()));
            DebugUtils.Assert(e.getMessage_cn() != null, String.format("message_cn is null for elaboration: %s", e.getRegex()));
        }
    }

    private static void retress(List<ErrorCodeElaboration> els) {
        if (retrees != null) {
            retrees = null;
        }
        List<String> r = new ArrayList<>();
        for (ErrorCodeElaboration e: els) {
            if (ElaborationSearchMethod.distance == e.getMethod()) {
                continue;
            }
            r.add(e.getRegex());
        }
        if (!r.isEmpty()) {
            retrees = new ReTree(r.toArray(new String[0]));
        }
    }

    private static void pattern(List<ErrorCodeElaboration> els) {
        patterns.clear();
        for (ErrorCodeElaboration e: els) {
            if (ElaborationSearchMethod.distance == e.getMethod()) {
                continue;
            }
            patterns.put(e.getRegex(), Pattern.compile(e.getRegex(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE));
        }
    }

    @SuppressWarnings("unchecked")
    private static List<ErrorCodeElaboration> initialElaborations() {
        List<String> errorTemplates = PathUtil.scanFolderOnClassPath(elaborateFolder);
        List<ErrorCodeElaboration> els = Collections.synchronizedList(new ArrayList<>());
        logger.info("start initializing elaborations");

        for (String errorTemplate: errorTemplates) {
            String name = PathUtil.fileName(errorTemplate);
            if (!name.endsWith(".json")) {
                logger.warn(String.format("found non json file [%s] in path: %s", name, errorTemplate));
                continue;
            }
            File templateFile = new File(errorTemplate);
            try {
                String content = FileUtils.readFileToString(templateFile, Charset.defaultCharset());
                if (content == null || content.isEmpty()) {
                    continue;
                }
                els.addAll(JSONObjectUtil.toCollection(
                        content,
                        ArrayList.class,
                        ErrorCodeElaboration.class
                ));
                els.sort(Comparator.comparingInt(
                        o -> Integer.parseInt(o.getCode())
                ));
            } catch (IOException e) {
                throw new RuntimeException(String.format("read error elaboration template files failed, due to: %s", e.getMessage()));
            }
        }

        validate(els);
        pattern(els);
        retress(els);
        logger.info(String.format("finish initializing system elaborations, got %s elaborations", els.size()));
        return els;
    }

    /**
     * check if the regex is contained in the elaborations.
     *
     * @param regex regex
     * @return true if the regex is contained
     */
    public static boolean regexContained(String regex) {
        for (ErrorCodeElaboration e: elaborations) {
            if (e.getRegex().equals(regex)) {
                return true;
            }
        }
        return false;
    }

    /**
     * check if the error code is contained in the elaborations.
     *
     * @param code error code
     * @return true if the error code is contained
     */
    public static boolean errorCodeContained(String code) {
        for (ErrorCodeElaboration e: elaborations) {
            String errCode = e.getCategory() + "." + e.getCode();
            if (errCode.equals(code)) {
                return true;
            }
        }
        return false;
    }

    private static double levenshtein(String str, String sub) {
        Levenshtein l = new Levenshtein();
        return l.distance(str, sub);
    }

    private static double normalized(String str, String sub) {
        NormalizedLevenshtein l = new NormalizedLevenshtein();
        return l.distance(str, sub);
    }

    private static double weight(String str, String sub) {
        WeightedLevenshtein l = new WeightedLevenshtein((c, c1) -> 1.0);
        return l.distance(str, sub);
    }

    private static double damerau(String str, String sub) {
        Damerau l = new Damerau();
        return l.distance(str, sub);
    }

    private static double optimal(String str, String sub) {
        OptimalStringAlignment l = new OptimalStringAlignment();
        return l.distance(str, sub);
    }

    private static double jaroWinkler(String str, String sub) {
        JaroWinkler l = new JaroWinkler();
        return l.distance(str, sub);
    }

    private static double longest(String str, String sub) {
        LongestCommonSubsequence l = new LongestCommonSubsequence();
        return l.length(str, sub);
    }

    private static double ngram(String str, String sub) {
        NGram l = new NGram(2);
        return l.distance(str, sub);
    }

    private static double metrics(String str, String sub) {
        MetricLCS l = new MetricLCS();
        return l.distance(str, sub);
    }

    private static final List<String> redundantErrors = CollectionDSL.list("unhandled exception happened when calling");

    private static boolean isRedundant(String sub) {
        for (String redundantError: redundantErrors) {
            if (sub.startsWith(redundantError)) {
                return true;
            }
        }
        return false;
    }

    private static void logSearchSpend(String sub, long start, boolean found) {
        logger.debug(String.format("[%s] spend %s ms to search elaboration \"%s\"", found, System.currentTimeMillis() - start,
                sub.length() > 50 ? sub.substring(0 , 50) + "..." : sub));
    }

    /**
     * find the most similar error code elaboration for the given error message.
     *
     * @param sub error message or error message fmt
     * @param args arguments
     * @return the most similar error code elaboration
     */
    public static ErrorCodeElaboration findSimilar(String sub, Object...args) {
        if (sub == null || sub.isEmpty() || isRedundant(sub)) {
            return null;
        }

        long start = System.currentTimeMillis();
        ErrorCodeElaboration err = errors.get(sub);
        if (err != null && verifyElaboration(err, sub, args)) {
            logSearchSpend(sub, start, true);
            return err;
        }

        // note: do not use another cache to support general error
        // fmt, because we think during a period only errors with
        // same cause will happen
        // invalid cache, generate elaboration again
        if (err != null) {
            errors.remove(sub);
        }

        if (args != null && missed.get(String.format(sub, args)) != null) {
            logSearchSpend(sub, start, false);
            return null;
        } else if (missed.get(sub) != null) {
            logSearchSpend(sub, start, false);
            return null;
        }

        try {
            logger.trace(String.format("start to search elaboration for: %s", String.format(sub, args)));
            err = findMostSimilarRegex(String.format(sub, args));
        } catch (Exception e) {
            logger.trace(String.format("start search elaboration for: %s", sub));
            err = findMostSimilarRegex(sub);
        }

        // find by distance is not reliable disable it for now
        if (err == null) {
            err = findSimilarDistance(sub);
        }

        logSearchSpend(sub, start, err != null);

        return err;
    }

    private static boolean verifyElaboration(ErrorCodeElaboration elaboration, String sub, Object...args) {
        try {
            if (elaboration.getMethod() == ElaborationSearchMethod.regex) {
                return isRegexMatched(elaboration.getRegex(), sub)
                        || isRegexMatched(elaboration.getRegex(), String.format(sub, args));
            } else if (elaboration.getMethod() == ElaborationSearchMethod.distance) {
                return getSimilar(elaboration.getRegex(), sub) < threshold
                        || getSimilar(elaboration.getRegex(), String.format(sub, args)) < threshold;
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    // better precision, worse performance
    private static ErrorCodeElaboration findMostSimilarRegex(String sub) {
        if (!isRegexMatchedByRetrees(sub)) {
            return null;
        }

        // find the most similar regex by compare matched regex length
        return elaborations.stream()
                .filter(ela -> ElaborationSearchMethod.regex == ela.getMethod())
                .filter(ela -> isRegexMatched(ela.getRegex(), sub))
                .max(Comparator.comparingInt(e -> e.getRegex().length()))
                .orElse(null);
    }

    private static ErrorCodeElaboration findSimilarDistance(String sub) {
        return elaborations.stream()
                .filter(ela -> ElaborationSearchMethod.distance == ela.getMethod())
                .map(ela -> {
                    ErrorCodeElaboration result = ErrorCodeElaboration.clone(ela);
                    result.setDistance(getSimilar(ela.getRegex(), sub));
                    return result;
                })
                .min(Comparator.comparingDouble(ErrorCodeElaboration::getDistance))
                .orElse(null);
    }

    /**
     * format the error code elaboration message
     *
     * @param message error code elaboration message
     * @param args arguments
     * @return formatted error code elaboration message
     */
    public static String formatElaborationDeprecated(String message, Object...args) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(message);

        buffer.deleteCharAt(buffer.lastIndexOf("\n"));
        return String.format(buffer.toString(), args);
    }

    public static String formatElaboration(String message, Object...args) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(message);
        return String.format(buffer.toString(), args);
    }

    private static boolean isRegexMatchedByRetrees(String sub) {
        return retrees != null && new ReMatcher(retrees, sub).find();
    }

    /**
     * check if the given string is matched by the given regex.
     *
     * @param regex regex
     * @param sub string
     * @return true if the given string is matched by the given regex
     */
    public static boolean isRegexMatched(String regex, String sub) {
        if (patterns.get(regex) == null) {
            return false;
        }
        return patterns.get(regex).matcher(sub).find();
    }

    /**
     * Jaro Similarity is the measure of similarity between two strings.
     *
     * The value of Jaro distance ranges from 0 to 1. where 0 means the strings are equal
     * and 1 means no similarity between the two strings.
     *
     * We need to check the result of Jaro Similarity is greater than the threshold value.
     *
     * In case of a very long sub, the result is not reliable
     *
     * @param str regex
     * @param sub string
     * @return Jaro Similarity value
     */
    private static double getSimilar(String str, String sub) {
        return getSimilar(str, sub, "jaroWinkler");
    }

    private static double getLength(String str, String sub) {
        return getSimilar(str, sub, "optimal");
    }

    private static double getSimilar(String str, String sub, String algorithm) {
        switch (algorithm) {
            case "levenshtein": return levenshtein(str, sub);
            case "normalized": return normalized(str, sub);
            case "weight": return weight(str, sub);
            case "damerau": return damerau(str, sub);
            case "optimal": return optimal(str, sub);
            case "jaroWinkler": return jaroWinkler(str, sub);
            case "longest": return longest(str, sub);
            case "ngram": return ngram(str, sub);
            case "metrics": return metrics(str, sub);
            default: throw new RuntimeException(String.format("error algorithm: %s", algorithm));
        }
    }
}

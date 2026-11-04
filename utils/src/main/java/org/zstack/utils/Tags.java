package org.zstack.utils;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Tags {
    public static final String SEPARATOR = "::";

    public static boolean hasSeparator(String tag) {
        return tag != null && tag.contains(SEPARATOR);
    }

    /**
     * <code>
     *     join("aaa", "bbb", "ccc") = "aaa::bbb::ccc"
     * </code>
     */
    public static String join(String... tokens) {
        checkTokens(tokens);
        return StringUtils.join(tokens, SEPARATOR);
    }

    public static String join(Collection<String> tokens) {
        if (tokens == null) {
            throw new IllegalArgumentException("tag tokens cannot be null");
        }
        return join(tokens.toArray(new String[0]));
    }

    /**
     * <code>
     *     prefix("aaa", "bbb") = "aaa::bbb::"
     * </code>
     */
    public static String prefix(String... tokens) {
        return join(tokens) + SEPARATOR;
    }

    public static String prefix(Collection<String> tokens) {
        return join(tokens) + SEPARATOR;
    }

    public static String placeholder(String token) {
        checkTokens(token);
        return "{" + token + "}";
    }

    public static String[] split(String tag) {
        if (tag == null) {
            throw new IllegalArgumentException("tag cannot be null");
        }
        return tag.split(SEPARATOR);
    }

    /**
     * <code>
     *     first("aaa::bbb::ccc") = "aaa"
     * <br/>
     *     first("xxx") = "xxx"
     * <br/>
     *     first("::xxx") = ""
     * </code>
     */
    public static String first(String tag) {
        if (tag == null) {
            throw new IllegalArgumentException("tag cannot be null");
        }
        int index = tag.indexOf(SEPARATOR);
        return index < 0 ? tag : tag.substring(0, index);
    }

    private static void checkTokens(String... tokens) {
        if (tokens == null) {
            throw new IllegalArgumentException("tag tokens cannot be null");
        }
        for (String token : tokens) {
            if (token == null || token.isEmpty()) {
                throw new IllegalArgumentException("tag token cannot be empty");
            }
            if (token.contains(SEPARATOR)) {
                throw new IllegalArgumentException(String.format(
                        "tag token cannot contain \"%s\": %s", SEPARATOR, token));
            }
        }
    }

    // ------------------ Methods for SystemTagVO ------------------

    public static Map<String, String> parse(String fmt, String tag) {
        ArrayList<String> origins = new ArrayList<>();
        Collections.addAll(origins, split(tag));

        ArrayList<String> t = new ArrayList<>();
        Collections.addAll(t, split(fmt));

        HashMap<String, String> ret = new HashMap<>();
        for (int i = 0; i < t.size(); i++) {
            String key = t.get(i);
            if (!key.startsWith("{") || !key.endsWith("}")) {
                continue;
            }

            key = key.replaceAll("\\{", "").replaceAll("\\}", "");
            if (i < origins.size()) {
                ret.put(key, origins.get(i));
            }
        }

        return ret;
    }

    public static boolean isMatch(String fmt, String tag) {
        char fmtFirstChar = fmt.charAt(0);
        if (tag != null && !tag.isEmpty() && fmtFirstChar != tag.charAt(0) && fmtFirstChar != '{') {
            return false;
        }

        ArrayList<String> origins =  new ArrayList<>();
        Collections.addAll(origins, split(tag));

        ArrayList<String> t = new ArrayList<>();
        Collections.addAll(t, split(fmt));

        if (!hasSeparator(fmt)) {
            return fmt.equals(tag);
        }

        if (origins.size() != t.size()) {
            return false;
        }

        for (int i = 0; i < t.size(); i++) {
            String fmtKey = t.get(i);
            if (fmtKey.startsWith("{") && fmtKey.endsWith("}")) {
                continue;
            }

            String originKey = origins.get(i);
            if (!originKey.equals(fmtKey)) {
                return false;
            }
        }

        return true;
    }

    public static Map<String, String> parseIfMatch(String fmt, String tag) {
        if (!isMatch(fmt, tag)) {
            return null;
        }

        return parse(fmt, tag);
    }

    public static String tagPatternToSqlPattern(String tag) {
        return tag.replaceAll("\\{(.+?)\\}", "%");
    }
}

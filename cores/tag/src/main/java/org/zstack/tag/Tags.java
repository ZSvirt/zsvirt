package org.zstack.tag;

import org.apache.commons.lang.StringUtils;

import java.util.Collection;

public class Tags {
    public static final String SEPARATOR = "::";

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
}

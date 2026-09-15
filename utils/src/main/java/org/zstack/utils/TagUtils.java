package org.zstack.utils;

import java.util.*;

/**
 * use {@link Tags}
 */
@Deprecated
public class TagUtils {
    public static Map<String, String> parse(String fmt, String tag) {
        return Tags.parse(fmt, tag);
    }

    public static boolean isMatch(String fmt, String tag) {
        return Tags.isMatch(fmt, tag);
    }

    public static Map<String, String> parseIfMatch(String fmt, String tag) {
        return Tags.parseIfMatch(fmt, tag);
    }

    public static String tagPatternToSqlPattern(String tag) {
        return Tags.tagPatternToSqlPattern(tag);
    }
}

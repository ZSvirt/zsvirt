package org.zstack.utils;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.logging.log4j.util.Strings;

import java.util.*;

/**
 */
public class TagUtils {
    public static Map<String, String> parse(String fmt, String tag) {
        List<String> origins =  new ArrayList<String>();
        Collections.addAll(origins, tag.split("::"));

        List<String> t = new ArrayList<String>();
        Collections.addAll(t, fmt.split("::"));

        Map<String, String> ret = new HashMap();
        for (int i=0;i<t.size(); i++) {
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
        if (!Strings.isEmpty(tag) && fmtFirstChar != tag.charAt(0) && fmtFirstChar != '{') {
            return false;
        }

        List<String> origins =  new ArrayList<String>();
        Collections.addAll(origins, tag.split("::"));

        List<String> t = new ArrayList<String>();
        Collections.addAll(t, fmt.split("::"));

        if (fmt.indexOf("::") == -1) {
            return fmt.equals(tag);
        }

        if (origins.size() != t.size()) {
            return false;
        }

        for (int i=0; i<t.size(); i++) {
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

    public static String tagPatternToSqlPattern(Enum tag) {
        return tagPatternToSqlPattern(tag.toString());
    }

    public static String tagPatternToSqlPattern(String tag) {
        return tag.replaceAll("\\{(.+?)\\}", "%");
    }
}

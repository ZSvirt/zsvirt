package org.zstack.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DomainUtils {
    private static final String regex = "(https?://)?([^:/]+)(:\\d+)?";

    public static String getDomain(String url) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "";
    }
}

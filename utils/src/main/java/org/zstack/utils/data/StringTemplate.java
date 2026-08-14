package org.zstack.utils.data;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringTemplate {
    public static String substitute(String text, Map<String, String> tokens) {
        Pattern pattern = Pattern.compile("\\{(.+?)\\}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = tokens.get(matcher.group(1));
            if (replacement != null) {
                matcher.appendReplacement(buffer, "");
                buffer.append(replacement);
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
    
    public static String join(Collection<String> lst, String delimiter) {
        Iterator i = lst.iterator();
        StringBuilder sb = new StringBuilder();
        while (true) {
            sb.append(i.next());
            if (i.hasNext()) {
                sb.append(delimiter);
            } else {
                break;
            }
        }
        return sb.toString();
    }
}

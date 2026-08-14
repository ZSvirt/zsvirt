package org.zstack.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class StringDSL {
    public static class StringWrapper {
        private String self;

        public StringWrapper(String self) {
            this.self = self;
        }

        public StringWrapper(String...strs) {
            self = "";
            for (String str : strs) {
                self += str;
            }
        }

        @Override
        public String toString() {
            return self;
        }

        private String substitute(Map<String, Object> tokens) {
            Pattern pattern = Pattern.compile("\\{(.+?)\\}");
            Matcher matcher = pattern.matcher(self);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                Object replacement = tokens.get(matcher.group(1));
                if (replacement != null) {
                    matcher.appendReplacement(buffer, "");
                    buffer.append(replacement.toString());
                }
            }
            matcher.appendTail(buffer);
            return buffer.toString();
        }

        public String format(Object...params) {
            Map<String, Object> tokens = new HashMap<String, Object>();
            for (int i=0; i<params.length; i++) {
                Object p = params[i];
                tokens.put(String.valueOf(i), p != null ? p.toString() : "null");
            }
            return substitute(tokens);
        }

        public String formatByMap(Map tokens) {
            return substitute(tokens);
        }
    }

    public static StringWrapper s(String...strs) {
        return new StringWrapper(strs);
    }

    public static StringWrapper ln(String...strs) {
        String[] ss = new String[strs.length];
        for (int i=0; i<strs.length; i++) {
            ss[i] = strs[i] + '\n';
        }

        return new StringWrapper(ss);
    }

    public static boolean equals(String a, String b) {
        if (a == null && b == null) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }

    public static String stripStart(String str, String prefix) {
        if (!str.startsWith(prefix)) {
            return str;
        }

        return str.substring(prefix.length());
    }

    public static String stripEnd(String str, String suffix) {
        if (!str.endsWith(suffix)) {
            return str;
        }

        return str.substring(0, str.indexOf(suffix));
    }

    public static String splitCamelCase(String s) {
        return s.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );
    }

    public static String inputStreamToString(InputStream in) {
        try {
            StringWriter writer = new StringWriter();
            IOUtils.copy(in, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getMd5Sum(String content) {
        if (content == null) {
            throw new RuntimeException("cannot get md5sum from null");
        }
        try {
            return DigestUtils.md5Hex(new ByteArrayInputStream(content.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(String.format("read input stream err: %s", e.getMessage()));
        }
    }

    public static boolean isUuid(String uuid) {
        return uuid != null && uuid.toLowerCase().matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

    public static boolean isZStackUuid(String uuid) {
        return uuid != null && uuid.matches("[0-9a-f]{8}[0-9a-f]{4}[1-5][0-9a-f]{3}[89ab][0-9a-f]{3}[0-9a-f]{12}");
    }

    public static boolean isApiId(String apiId) {
        return apiId != null && apiId.matches("[0-9a-f]{8}[0-9a-f]{4}[0-9a-f]{4}[0-9a-f]{4}[0-9a-f]{12}");
    }

    public static String createFixedUuid(Class<?> voClass) {
        return createFixedUuid(voClass.getSimpleName());
    }

    public static String createFixedUuid(String text) {
        StringBuilder builder = new StringBuilder(
                UUID.nameUUIDFromBytes(text.getBytes()).toString().replace("-", ""));
        builder.setCharAt(0, 'f');
        builder.setCharAt(1, 'f');
        builder.setCharAt(2, '0');
        builder.setCharAt(30, '0');
        builder.setCharAt(31, '0');
        return builder.toString();
    }

    private static int hashOXR(List lst) {
        return lst.stream().mapToInt(Object::hashCode)
                .reduce(0, (l, r) -> l ^ r);
    }

    public static boolean stringCompareInLineOrderIndpendent(String str1, String str2) {
        return hashOXR(Arrays.asList(str1.split("\n"))) == hashOXR(Arrays.asList(str2.split("\n")));
    }

    public static String transToTfUuid(String zUuid){
        StringBuilder buffer = new StringBuilder(zUuid);
        buffer.insert(20, '-');
        buffer.insert(16, '-');
        buffer.insert(12, '-');
        buffer.insert(8, '-');
        return buffer.toString();
    }

    public static String transToZstackUuid(String tfUuid){
        return tfUuid.replaceAll("-","");
    }
}

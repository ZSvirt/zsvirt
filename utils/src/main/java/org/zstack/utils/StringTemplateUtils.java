package org.zstack.utils;

import groovy.text.GStringTemplateEngine;
import groovy.text.Template;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class StringTemplateUtils {
    private static final GStringTemplateEngine engine = new GStringTemplateEngine();

    private static final Map<Integer, Template> stringTemplateMap = new HashMap<>();

    public static String createStringFromTemplate(String template, Map<String, Object> bindings) {
        Template t = stringTemplateMap.get(template.hashCode());

        if (t != null) {
            return t.make(bindings).toString();
        }

        try {
            t = engine.createTemplate(new StringReader(template));
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }

        stringTemplateMap.put(template.hashCode(), t);
        return t.make(bindings).toString();
    }
}

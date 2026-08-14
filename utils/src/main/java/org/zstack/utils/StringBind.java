package org.zstack.utils;

import java.util.HashMap;
import java.util.Map;

import static org.zstack.utils.StringDSL.ln;

/**
 * Created by xing5 on 2016/6/16.
 */
public class StringBind {
    private String template;
    private Map<String, Object> tokens = new HashMap<>();

    public StringBind(String template) {
        this.template = template;
    }

    public StringBind bind(String key, Object value) {
        tokens.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return ln(template).formatByMap(tokens);
    }
}


package org.zstack.header.identity.login;

import java.util.HashMap;
import java.util.Map;

public class LoginAuthenticationProcedureDesc {
    private int order = 0;
    private String name;
    private Map<String, String> properties = new HashMap<>();

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public void addProperty(String key, String value) {
        this.properties.put(key, value);
    }
}

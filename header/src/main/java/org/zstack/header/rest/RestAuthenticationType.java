package org.zstack.header.rest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RestAuthenticationType {
    private static Map<String, RestAuthenticationType> types = Collections.synchronizedMap(new HashMap<String, RestAuthenticationType>());

    private final String typeName;

    public RestAuthenticationType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static RestAuthenticationType valueOf(String typeName) {
        RestAuthenticationType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("RestAuthenticationType type: " + typeName + " was not registered by any RestAuthenticationBackend");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof RestAuthenticationType)) {
            return false;
        }

        RestAuthenticationType type = (RestAuthenticationType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }
}

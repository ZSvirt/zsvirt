package org.zstack.core.encrypt;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EncryptDriverType {
    private static final Map<String, EncryptDriverType> types = Collections.synchronizedMap(new HashMap<>());
    private final String typeName;

    public EncryptDriverType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static boolean hasType(String type) {
        return types.containsKey(type);
    }

    public static EncryptDriverType valueOf(String typeName) {
        EncryptDriverType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("Encrypt driver type: " + typeName + " was not registered by any EncryptDriver");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }
}

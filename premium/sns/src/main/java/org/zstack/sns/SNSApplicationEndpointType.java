package org.zstack.sns;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SNSApplicationEndpointType {
    private static Map<String, SNSApplicationEndpointType> types = Collections.synchronizedMap(new HashMap<>());
    private final String typeName;

    public SNSApplicationEndpointType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static boolean hasType(String type) {
        return types.containsKey(type);
    }

    public static SNSApplicationEndpointType valueOf(String typeName) {
        SNSApplicationEndpointType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("SNSApplicationPlatformType type: " + typeName + " was not registered by any SNSApplicationPlatformFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof SNSApplicationEndpointType)) {
            return false;
        }

        SNSApplicationEndpointType type = (SNSApplicationEndpointType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }
}

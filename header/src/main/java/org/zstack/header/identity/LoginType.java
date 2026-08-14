package org.zstack.header.identity;

import org.zstack.header.host.HypervisorType;

import java.util.*;

/**
 * Created by kayo on 2018/7/11.
 */
public class LoginType {
    private static Map<String, LoginType> types = Collections.synchronizedMap(new HashMap<String, LoginType>());
    private final String typeName;
    private boolean exposed = true;

    public LoginType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public LoginType(String typeName, boolean exposed) {
        this(typeName);
        this.exposed = exposed;
    }

    public static boolean hasType(String type) {
        return types.keySet().contains(type);
    }

    public static LoginType valueOf(String typeName) {
        LoginType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("LoginType type: " + typeName + " was not registered by any HypervisorFactory");
        }
        return type;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof HypervisorType)) {
            return false;
        }

        LoginType type = (LoginType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        HashSet<String> exposedTypes = new HashSet<String>();
        for (LoginType type : types.values()) {
            if (type.exposed) {
                exposedTypes.add(type.toString());
            }
        }
        return exposedTypes;
    }
}

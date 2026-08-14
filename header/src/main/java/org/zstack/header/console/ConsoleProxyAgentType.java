package org.zstack.header.console;

import java.util.*;

/**
 * Created by xing5 on 2016/3/15.
 */
public class ConsoleProxyAgentType {
    private static Map<String, ConsoleProxyAgentType> types = Collections.synchronizedMap(new HashMap<String, ConsoleProxyAgentType>());
    private final String typeName;

    public ConsoleProxyAgentType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static boolean hasType(String type) {
        return types.keySet().contains(type);
    }

    public static ConsoleProxyAgentType valueOf(String typeName) {
        ConsoleProxyAgentType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("Console proxy agent type: " + typeName + " was not registered");
        }
        return type;
    }


    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof ConsoleProxyAgentType)) {
            return false;
        }

        ConsoleProxyAgentType type = (ConsoleProxyAgentType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        HashSet<String> exposedTypes = new HashSet<String>();
        for (ConsoleProxyAgentType type : types.values()) {
            exposedTypes.add(type.toString());
        }
        return exposedTypes;
    }
}

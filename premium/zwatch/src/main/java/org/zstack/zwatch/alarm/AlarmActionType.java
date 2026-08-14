package org.zstack.zwatch.alarm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AlarmActionType {
    private static Map<String, AlarmActionType> types = Collections.synchronizedMap(new HashMap<>());
    private final String typeName;

    public AlarmActionType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static Map<String, AlarmActionType> getAllTypes() {
        return types;
    }

    public static AlarmActionType valueOf(String typeName) {
        AlarmActionType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("AlarmActionType type: " + typeName + " was not registered by any AlarmActionFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof AlarmActionType)) {
            return false;
        }

        AlarmActionType type = (AlarmActionType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }
}

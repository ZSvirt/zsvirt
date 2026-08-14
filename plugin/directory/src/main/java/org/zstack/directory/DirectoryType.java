package org.zstack.directory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author shenjin
 * @date 2022/12/11 20:00
 */
public class DirectoryType {
    private static Map<String, DirectoryType> types = Collections.synchronizedMap(new HashMap<String, DirectoryType>());
    private final String typeName;

    public DirectoryType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static DirectoryType valueOf(String typeName) {
        DirectoryType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("DirectoryType type: " + typeName + " was not registered by any DirectoryChecker");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof DirectoryType)) {
            return false;
        }

        DirectoryType type = (DirectoryType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }
}

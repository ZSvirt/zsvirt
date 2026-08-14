package org.zstack.header.affinitygroup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AffinityGroupSubType {
    private static Map<String, AffinityGroupSubType> types = Collections.synchronizedMap(new HashMap<String, AffinityGroupSubType>());
    private final String typeName;

    public AffinityGroupSubType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static AffinityGroupSubType valueOf(String typeName) {
        AffinityGroupSubType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("AffinityGroupSubType type: " + typeName + " was not registered by any one component");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof AffinityGroupSubType)) {
            return false;
        }

        AffinityGroupSubType type = (AffinityGroupSubType)t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

}

package org.zstack.header.network.l3;

import org.zstack.utils.DebugUtils;

import java.util.*;

public class L3NetworkType {
    private static Map<String, L3NetworkType> types = Collections.synchronizedMap(new HashMap<String, L3NetworkType>());
    private final String typeName;
    private boolean exposed;
    private boolean mandatoryIpAllocation;

    public L3NetworkType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    static L3NetworkType buildL3NetworkType(String typeName, boolean exposed,
                                            boolean mandatoryIpAllocation) {
        L3NetworkType type = new L3NetworkType(typeName);
        type.exposed = exposed;
        type.mandatoryIpAllocation = mandatoryIpAllocation;
        return type;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public boolean isMandatoryIpAllocation() {
        return mandatoryIpAllocation;
    }

    public void setMandatoryIpAllocation(boolean mandatoryIpAllocation) {
        this.mandatoryIpAllocation = mandatoryIpAllocation;
    }

    public boolean enableRa() {
        return !mandatoryIpAllocation;
    }

    public static boolean hasType(String typeName) {
        return types.containsKey(typeName);
    }

    public static L3NetworkType valueOf(String typeName) {
        L3NetworkType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("L3NetworkType type: " + typeName + " was not registered by any L3NetworkFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof L3NetworkType)) {
            return false;
        }

        L3NetworkType type = (L3NetworkType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        HashSet<String> exposedTypes = new HashSet<String>();
        for (L3NetworkType type : types.values()) {
            if (type.isExposed()) {
                exposedTypes.add(type.toString());
            }
        }
        return exposedTypes;
    }

    public static class L3NetworkTypeBuilder {
        private String typeName;
        private boolean exposed = true;
        private boolean mandatoryIpAllocation = true;

        public L3NetworkType.L3NetworkTypeBuilder typeName(String typeName) {
            this.typeName = typeName;
            return this;
        }

        public L3NetworkType.L3NetworkTypeBuilder exposed(boolean exposed) {
            this.exposed = exposed;
            return this;
        }

        public L3NetworkType.L3NetworkTypeBuilder mandatoryIpAllocation(boolean mandatoryIpAllocation) {
            this.mandatoryIpAllocation = mandatoryIpAllocation;
            return this;
        }

        public L3NetworkType build() {
            DebugUtils.Assert(typeName != null, "type name is mandatory");
            return buildL3NetworkType(typeName, exposed, mandatoryIpAllocation);
        }
    }
}

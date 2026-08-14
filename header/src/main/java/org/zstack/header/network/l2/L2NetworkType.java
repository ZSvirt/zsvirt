package org.zstack.header.network.l2;

import org.zstack.utils.DebugUtils;

import java.util.*;

public class L2NetworkType {
    private static Map<String, L2NetworkType> types = Collections.synchronizedMap(new HashMap<String, L2NetworkType>());
    private final String typeName;
    private boolean exposed;
    private boolean sriovSupported;
    // whether the l2 is attached to all hosts in the cluster at once
    private boolean attachToAllHosts = true;

    public static boolean hasType(String typeName) {
        return types.containsKey(typeName);
    }

    public L2NetworkType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    static L2NetworkType buildL2NetworkType(String typeName, boolean exposed,
                                            boolean sriovSupported, boolean attachToHostSupported) {
        L2NetworkType type = new L2NetworkType(typeName);
        type.exposed = exposed;
        type.sriovSupported = sriovSupported;
        type.attachToAllHosts = attachToHostSupported;
        return type;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public boolean isSriovSupported() {
        return sriovSupported;
    }

    public void setSriovSupported(boolean isSupportSriov) {
        this.sriovSupported = isSupportSriov;
    }

    public boolean isAttachToAllHosts() {
        return attachToAllHosts;
    }

    public void setAttachToAllHosts(boolean attachToAllHosts) {
        this.attachToAllHosts = attachToAllHosts;
    }

    public static L2NetworkType valueOf(String typeName) {
        L2NetworkType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("L2NetworkType type: " + typeName + " was not registered by any L2NetworkFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof L2NetworkType)) {
            return false;
        }

        L2NetworkType type = (L2NetworkType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        HashSet<String> exposedTypes = new HashSet<>();
        for (L2NetworkType type : types.values()) {
            if (type.isExposed()) {
                exposedTypes.add(type.toString());
            }
        }
        return exposedTypes;
    }

    public static Set<String> getSriovSupportedTypeNames() {
        HashSet<String> supportedTypes = new HashSet<>();
        for (L2NetworkType type : types.values()) {
            if (type.isSriovSupported()) {
                supportedTypes.add(type.toString());
            }
        }
        return supportedTypes;
    }

    public static class L2NetworkTypeBuilder {
        private String typeName;
        private boolean exposed = true;
        private boolean sriovSupported = false;
        private boolean attachToAllHosts = true;

        public L2NetworkTypeBuilder typeName(String typeName) {
            this.typeName = typeName;
            return this;
        }

        public L2NetworkTypeBuilder exposed(boolean exposed) {
            this.exposed = exposed;
            return this;
        }

        public L2NetworkTypeBuilder sriovSupported(boolean sriovSupported) {
            this.sriovSupported = sriovSupported;
            return this;
        }

        public L2NetworkTypeBuilder attachToAllHosts(boolean attachToAllHosts) {
            this.attachToAllHosts = attachToAllHosts;
            return this;
        }

        public L2NetworkType build() {
            DebugUtils.Assert(typeName != null, "type name is mandatory");
            return buildL2NetworkType(typeName, exposed, sriovSupported, attachToAllHosts);
        }
    }
}

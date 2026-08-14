package org.zstack.header.identity.login;

import java.util.*;

public class AdditionalAuthFeature {
    private static final Map<String, AdditionalAuthFeature> types = Collections.synchronizedMap(new HashMap<String, AdditionalAuthFeature>());
    private final String typeName;

    public AdditionalAuthFeature(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static boolean hasType(String type) {
        return types.containsKey(type);
    }

    public static AdditionalAuthFeature valueOf(String typeName) {
        AdditionalAuthFeature type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException(String.format("AdditionalAuthFeature type: %s not supported", typeName));
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (!(t instanceof AdditionalAuthFeature)) {
            return false;
        }

        AdditionalAuthFeature type = (AdditionalAuthFeature) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        HashSet<String> exposedTypes = new HashSet<>();
        for (AdditionalAuthFeature type : types.values()) {
            exposedTypes.add(type.toString());
        }
        return exposedTypes;
    }
}

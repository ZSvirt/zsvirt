package org.zstack.header.image;

import java.util.*;


public class ImageType {
    private static Map<String, ImageType> types = Collections.synchronizedMap(new HashMap<String, ImageType>());
    private final String typeName;
    private boolean exposed = true;

    public ImageType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public ImageType(String typeName, boolean exposed) {
        this(typeName);
        this.exposed = exposed;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public static boolean hasType(String type) {
        return types.keySet().contains(type);
    }

    public static ImageType valueOf(String typeName) {
        ImageType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("ImageType type: " + typeName + " was not registered by any ImageFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof ImageType)) {
            return false;
        }

        ImageType type = (ImageType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        HashSet<String> exposedTypes = new HashSet<String>();
        for (ImageType type : types.values()) {
            if (type.isExposed()) {
                exposedTypes.add(type.toString());
            }
        }
        return exposedTypes;
    }
}

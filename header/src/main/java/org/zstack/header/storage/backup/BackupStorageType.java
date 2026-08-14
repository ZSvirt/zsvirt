package org.zstack.header.storage.backup;

import java.util.*;

import static org.zstack.utils.CollectionUtils.transform;


public class BackupStorageType {
    private static Map<String, BackupStorageType> types = Collections.synchronizedMap(new HashMap<>());
    private final String typeName;
    private final Set<String> supportedSchemes;
    private boolean exposed = true;
    private int order;
    private BackupStorageFindRelatedPrimaryStorage primaryStorageFinder;

    public BackupStorageType(String typeName, String... protocols) {
        this.typeName = typeName;
        supportedSchemes = new HashSet<String>(protocols.length);
        Collections.addAll(supportedSchemes, protocols);
        types.put(typeName, this);
    }

    public BackupStorageType(String typeName, boolean expose, String... protocols) {
        this(typeName);
        this.exposed = expose;
    }

    public Set<String> getSupportedSchemes() {
        return supportedSchemes;
    }

    public static BackupStorageType valueOf(String typeName) {
        BackupStorageType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("BackupStorageType type: " + typeName + " was not registered by any BackupStorageFactory");
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
        if (!(t instanceof BackupStorageType)) {
            return false;
        }

        BackupStorageType type = (BackupStorageType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static List<String> getAllTypeNames() {
        List<BackupStorageType> exposedTypes = new ArrayList<BackupStorageType>();
        for (BackupStorageType type : types.values()) {
            if (type.isExposed()) {
                exposedTypes.add(type);
            }
        }

        exposedTypes.sort(Comparator.comparingInt(BackupStorageType::getOrder));

        return transform(exposedTypes, BackupStorageType::toString);
    }

    public List<String> findRelatedPrimaryStorage(String bsUuid) {
        return primaryStorageFinder == null ? null : primaryStorageFinder.findRelatedPrimaryStorage(bsUuid);
    }

    public BackupStorageFindRelatedPrimaryStorage getPrimaryStorageFinder() {
        return primaryStorageFinder;
    }

    public void setPrimaryStorageFinder(BackupStorageFindRelatedPrimaryStorage primaryStorageFinder) {
        this.primaryStorageFinder = primaryStorageFinder;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}

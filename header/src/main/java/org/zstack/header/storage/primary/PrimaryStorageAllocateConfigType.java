package org.zstack.header.storage.primary;

/**
 * Created by lining on 2019/4/24.
 */
public enum PrimaryStorageAllocateConfigType {
    LOCAL(DefaultPrimaryStorageAllocateConfig.class, "LocalStorage"),
    NFS(DefaultPrimaryStorageAllocateConfig.class, "NFS"),
    SHBK(DefaultPrimaryStorageAllocateConfig.class, "SharedBlock"),
    Block(DefaultPrimaryStorageAllocateConfig.class, "Block"),
    CEPH(CephPrimaryStorageAllocateConfig.class, "Ceph");

    private Class<? extends PrimaryStorageAllocateConfig> type;
    private String category;

    public Class<? extends PrimaryStorageAllocateConfig> getType() {
        return type;
    }

    public void setType(Class<? extends PrimaryStorageAllocateConfig> type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    PrimaryStorageAllocateConfigType(Class<? extends PrimaryStorageAllocateConfig> type , String category) {
        this.type = type;
        this.category = category;
    }

    public static PrimaryStorageAllocateConfigType getByProductCategory(String category){
        for(PrimaryStorageAllocateConfigType t : values()){
            if(t.category.equalsIgnoreCase(category)){
                return t;
            }
        }

        throw new IllegalArgumentException(String.format("primaryStorage type[%s] not support", category));
    }
}

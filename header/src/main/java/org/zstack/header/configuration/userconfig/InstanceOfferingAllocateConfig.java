package org.zstack.header.configuration.userconfig;

import org.zstack.header.storage.primary.PrimaryStorageAllocateConfig;

/**
 * Created by lining on 2019/4/17.
 */
public class InstanceOfferingAllocateConfig {
    private PrimaryStorageAllocateConfig primaryStorage;

    private String clusterUuid;

    public PrimaryStorageAllocateConfig getPrimaryStorage() {
        return primaryStorage;
    }

    public void setPrimaryStorage(PrimaryStorageAllocateConfig primaryStorage) {
        this.primaryStorage = primaryStorage;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }
}

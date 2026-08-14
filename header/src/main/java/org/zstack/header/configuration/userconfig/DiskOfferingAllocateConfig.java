package org.zstack.header.configuration.userconfig;

import org.zstack.header.storage.primary.PrimaryStorageAllocateConfig;
import org.zstack.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lining on 2019/4/17.
 */
public class DiskOfferingAllocateConfig {
    private PrimaryStorageAllocateConfig primaryStorage;
    private List<PrimaryStorageAllocateConfig> primaryStorages;

    @Deprecated
    public List<PrimaryStorageAllocateConfig> getPrimaryStorages() {
        return primaryStorages;
    }

    public void setPrimaryStorages(List<PrimaryStorageAllocateConfig> primaryStorages) {
        this.primaryStorages = primaryStorages;
    }

    public PrimaryStorageAllocateConfig getPrimaryStorage() {
        return primaryStorage;
    }

    public void setPrimaryStorage(PrimaryStorageAllocateConfig primaryStorage) {
        this.primaryStorage = primaryStorage;
    }

    public List<PrimaryStorageAllocateConfig> getAllPrimaryStorages() {
        List<PrimaryStorageAllocateConfig> results = new ArrayList<>();
        if (primaryStorages != null) {
            results.addAll(primaryStorages);
        }
        if (primaryStorage != null) {
            results.add(primaryStorage);
        }

        return results;
    }
}

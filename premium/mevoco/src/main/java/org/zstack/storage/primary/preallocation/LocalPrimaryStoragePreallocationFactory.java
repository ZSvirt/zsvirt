package org.zstack.storage.primary.preallocation;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.volume.VolumeProvisioningStrategy;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.storage.primary.local.LocalStoragePrimaryStorageGlobalConfig;

/**
 * author:kaicai.hu
 * Date:2019/8/20
 */
public class LocalPrimaryStoragePreallocationFactory extends FileSystemPreallocationFactory {
    @Autowired
    private ResourceConfigFacade rcf;

    public static String type = LocalStorageConstants.LOCAL_STORAGE_TYPE;

    @Override
    public String getPrimaryStorageType() {
        return type;
    }

    @Override
    public String getPreallocation(String primaryStorageUuid) {
        return rcf.getResourceConfigValue(LocalStoragePrimaryStorageGlobalConfig.QCOW2_ALLOCATION, primaryStorageUuid, String.class);
    }

    @Override
    public String getProvisioningStrategy(String primaryStorageUuid) {
        return judgeQcow2ProvisioningStrategy(primaryStorageUuid);
    }
}

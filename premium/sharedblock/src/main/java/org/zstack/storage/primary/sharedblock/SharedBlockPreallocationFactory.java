package org.zstack.storage.primary.sharedblock;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeProvisioningStrategy;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.primary.preallocation.PreallocationFactory;
import org.zstack.storage.volume.VolumeSystemTags;

/**
 * author:kaicai.hu
 * Date:2019/8/20
 */
public class SharedBlockPreallocationFactory implements PreallocationFactory {
    @Autowired
    private ResourceConfigFacade rcf;

    public static final String type = SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE;

    @Override
    public String getPrimaryStorageType() {
        return type;
    }

    @Override
    public String getPreallocation(String primaryStorageUuid) {
        return rcf.getResourceConfigValue(SharedBlockGlobalConfig.QCOW2_ALLOCATION, primaryStorageUuid, String.class);
    }

    @Override
    public String getProvisioningStrategy(String primaryStorageUuid) {
        String provisioning = VolumeSystemTags.PRIMARY_STORAGE_VOLUME_PROVISIONING_STRATEGY.getTokenByResourceUuid(
                primaryStorageUuid, PrimaryStorageVO.class, VolumeSystemTags.PRIMARY_STORAGE_VOLUME_PROVISIONING_STRATEGY_TOKEN);
        return provisioning == null ? VolumeProvisioningStrategy.ThickProvisioning.toString() : provisioning;
    }
}
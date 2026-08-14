package org.zstack.storage.primary.preallocation;

import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.volume.VolumeProvisioningStrategy;

public class ExternalPrimaryStoragePreallocationFactory implements PreallocationFactory {
    @Override
    public String getPrimaryStorageType() {
        return PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public String getPreallocation(String primaryStorageUuid) {
        // TODO
        return null;
    }

    @Override
    public String getProvisioningStrategy(String primaryStorageUuid) {
        // TODO
        return VolumeProvisioningStrategy.ThinProvisioning.toString();
    }
}

package org.zstack.storage.primary.preallocation;

import org.zstack.header.volume.VolumeProvisioningStrategy;

public abstract class FileSystemPreallocationFactory implements PreallocationFactory {
    protected String judgeQcow2ProvisioningStrategy(String primaryStorageUuid) {
        String preallocation = getPreallocation(primaryStorageUuid);
        if (preallocation.equals("falloc") || preallocation.equals("full")) {
            return VolumeProvisioningStrategy.ThickProvisioning.toString();
        } else {
            return VolumeProvisioningStrategy.ThinProvisioning.toString();
        }
    }
}

package org.zstack.storage.primary.preallocation;

import org.zstack.header.volume.VolumeProvisioningStrategy;
import org.zstack.storage.ceph.CephConstants;

public class CephPrimaryStoragePreallocationFactory implements PreallocationFactory {
    public static String type = CephConstants.CEPH_PRIMARY_STORAGE_TYPE;

    @Override
    public String getPrimaryStorageType() {
        return type;
    }

    @Override
    public String getPreallocation(String primaryStorageUuid) {
        return "none";
    }

    @Override
    public String getProvisioningStrategy(String primaryStorageUuid) {
        return VolumeProvisioningStrategy.ThinProvisioning.toString();
    }
}
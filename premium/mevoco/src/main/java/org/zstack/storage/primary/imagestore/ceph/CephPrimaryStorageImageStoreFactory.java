package org.zstack.storage.primary.imagestore.ceph;

import org.zstack.header.host.HostResizeVolumeExtensionPoint;
import org.zstack.header.host.HostResizeVolumeStruct;
import org.zstack.header.storage.primary.PrimaryStorage;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.VolumeTO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageFactory;

/**
 * Created by david on 8/9/16.
 */
public class CephPrimaryStorageImageStoreFactory extends CephPrimaryStorageFactory implements HostResizeVolumeExtensionPoint {
    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new CephPrimaryStorageImageStoreBackend(vo);
    }

    @Override
    public HostResizeVolumeStruct beforeKvmHostResizeVolume(HostResizeVolumeStruct struct, VolumeInventory vol, String hostUuid) {
        if (!struct.getInstallPath().startsWith(VolumeTO.CEPH)) {
            return struct;
        }

        struct.setDeviceType(VolumeTO.CEPH);
        return struct;
    }
}

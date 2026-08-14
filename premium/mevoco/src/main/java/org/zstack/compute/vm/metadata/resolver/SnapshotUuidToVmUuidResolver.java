package org.zstack.compute.vm.metadata.resolver;

import org.zstack.core.db.Q;
import org.zstack.header.storage.snapshot.VolumeSnapshotAO_;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.metadata.VmUuidFromApiResolver;
import org.zstack.header.volume.VolumeAO_;
import org.zstack.header.volume.VolumeVO;

/**
 * snapshotUuid → VolumeSnapshotVO.volumeUuid → VolumeVO.vmInstanceUuid.
 */
public class SnapshotUuidToVmUuidResolver implements VmUuidFromApiResolver {

    @Override
    public String resolveVmUuid(String fieldValue) {
        if (fieldValue == null) {
            return null;
        }

        String volumeUuid = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotAO_.uuid, fieldValue).select(VolumeSnapshotAO_.volumeUuid).findValue();
        if (volumeUuid == null) {
            return null;
        }

        String vmUuid = Q.New(VolumeVO.class).eq(VolumeAO_.uuid, volumeUuid).select(VolumeAO_.vmInstanceUuid).findValue();
        if (vmUuid == null) {
            vmUuid = Q.New(VolumeVO.class).eq(VolumeAO_.uuid, volumeUuid).select(VolumeAO_.lastVmInstanceUuid).findValue();
        }

        return vmUuid;
    }
}

package org.zstack.compute.vm.metadata.resolver;

import org.zstack.core.db.Q;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.header.vm.metadata.VmUuidFromApiResolver;

/**
 * snapshotGroupUuid → VolumeSnapshotGroupVO.vmInstanceUuid.
 */
public class SnapshotGroupUuidToVmUuidResolver implements VmUuidFromApiResolver {

    @Override
    public String resolveVmUuid(String fieldValue) {
        if (fieldValue == null) {
            return null;
        }

        return Q.New(VolumeSnapshotGroupVO.class)
                .eq(VolumeSnapshotGroupVO_.uuid, fieldValue)
                .select(VolumeSnapshotGroupVO_.vmInstanceUuid)
                .findValue();
    }
}

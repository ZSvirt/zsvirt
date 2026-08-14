package org.zstack.header.volume;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.snapshot.ConsistentType;
import org.zstack.header.storage.snapshot.SnapshotBackendOperation;
import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created by MaJin on 2021/6/23.
 */
public interface CreateVolumeSnapshotGroupMessage {
    String getResourceUuid();
    String getName();
    String getDescription();
    String getRootVolumeUuid();
    ConsistentType getConsistentType();
    SessionInventory getSession();
    default SnapshotBackendOperation getBackendOperation() {
        return SnapshotBackendOperation.FILE_CREATION;
    }

    void setVmInstance(VmInstanceInventory inventory);
    VmInstanceInventory getVmInstance();
}

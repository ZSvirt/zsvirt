package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.SnapshotBackendOperation;

import java.util.List;

/**
 * Created by MaJin on 2019/8/16.
 */
public class CheckVolumeSnapshotOperationOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private List<String> volumeUuids;
    private String vmInstanceUuid;
    private String primaryStorageUuid;
    private SnapshotBackendOperation operation;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public List<String> getVolumeUuids() {
        return volumeUuids;
    }

    public void setVolumeUuids(List<String> volumeUuids) {
        this.volumeUuids = volumeUuids;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public SnapshotBackendOperation getOperation() {
        return operation;
    }

    public void setOperation(SnapshotBackendOperation operation) {
        this.operation = operation;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}

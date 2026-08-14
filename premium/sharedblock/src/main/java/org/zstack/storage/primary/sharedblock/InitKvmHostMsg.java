package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageHostStatus;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

public class InitKvmHostMsg extends NeedReplyMessage implements PrimaryStorageMessage, SharedBlockGroupPrimaryStorageHypervisorSpecificMessage {
    private String primaryStorageUuid;
    private String hypervisorType;
    private String hostUuid;
    private PrimaryStorageHostStatus expectStatus;

    public PrimaryStorageHostStatus getExpectStatus() {
        return expectStatus;
    }

    public void setExpectStatus(PrimaryStorageHostStatus expectStatus) {
        this.expectStatus = expectStatus;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getHypervisorType() {
        return hypervisorType;
    }
}

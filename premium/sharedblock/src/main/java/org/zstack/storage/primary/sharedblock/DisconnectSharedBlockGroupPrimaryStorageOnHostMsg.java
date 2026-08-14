package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

public class DisconnectSharedBlockGroupPrimaryStorageOnHostMsg extends NeedReplyMessage implements PrimaryStorageMessage, SharedBlockGroupPrimaryStorageHypervisorSpecificMessage {
    private String primaryStorageUuid;
    private String hypervisorType;
    private String hostUuid;
    private Boolean stopService = false;

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

    public Boolean getStopService() {
        return stopService;
    }

    public void setStopService(Boolean stopService) {
        this.stopService = stopService;
    }
}

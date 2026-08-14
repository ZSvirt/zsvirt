package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.volume.VolumeProvisioningStrategy;

public class SharedBlockConvertVolumeProvisioningMsg extends NeedReplyMessage implements PrimaryStorageMessage, SharedBlockGroupPrimaryStorageHypervisorSpecificMessage {
    private String primaryStorageUuid;
    private String hypervisorType;
    private VolumeProvisioningStrategy provisioningStrategy;
    private String volumeUuid;
    //NOTE(weiw): it may be a volume or snapshot
    private String installPath;
    //NOTE(weiw): if specified, will execute at the host
    private String hostUuid = null;

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

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public VolumeProvisioningStrategy getProvisioningStrategy() {
        return provisioningStrategy;
    }

    public void setProvisioningStrategy(VolumeProvisioningStrategy provisioningStrategy) {
        this.provisioningStrategy = provisioningStrategy;
    }
}

package org.zstack.header.storage.primary;

import org.zstack.header.host.HostInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.message.ReplayableMessage;
import org.zstack.header.volume.VolumeInventory;

public class InstantiateVolumeOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage, ReplayableMessage {
    private HostInventory destHost;
    private VolumeInventory volume;
    private String primaryStorageUuid;
    private boolean skipIfExisting;
    private String allocatedInstallUrl;

    public String getAllocatedInstallUrl() {
        return allocatedInstallUrl;
    }

    public void setAllocatedInstallUrl(String allocatedInstallUrl) {
        this.allocatedInstallUrl = allocatedInstallUrl;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public HostInventory getDestHost() {
        return destHost;
    }

    public void setDestHost(HostInventory destHost) {
        this.destHost = destHost;
    }

    public boolean isSkipIfExisting() {
        return skipIfExisting;
    }

    public void setSkipIfExisting(boolean skipIfExisting) {
        this.skipIfExisting = skipIfExisting;
    }

    @Override
    public String getResourceUuid() {
        return volume.getUuid();
    }

    @Override
    public Class getReplayableClass() {
        return InstantiateVolumeOnPrimaryStorageMsg.class;
    }
}

package org.zstack.storage.primary.local;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeLuksAgentSpec;

/**
 * Created by frank on 10/24/2015.
 */
public class LocalStorageCreateEmptyVolumeMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String hostUuid;
    private String backingFile;
    private VolumeInventory volume;
    private VolumeLuksAgentSpec volumeLuksAgentSpec;

    public String getBackingFile() {
        return backingFile;
    }

    public void setBackingFile(String backingFile) {
        this.backingFile = backingFile;
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

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }

    public VolumeLuksAgentSpec getVolumeLuksAgentSpec() {
        return volumeLuksAgentSpec;
    }

    public void setVolumeLuksAgentSpec(VolumeLuksAgentSpec volumeLuksAgentSpec) {
        this.volumeLuksAgentSpec = volumeLuksAgentSpec;
    }
}

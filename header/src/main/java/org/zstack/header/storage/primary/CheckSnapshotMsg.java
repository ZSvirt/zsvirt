package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

import java.util.Map;

public class CheckSnapshotMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String volumeUuid;
    private String primaryStorageUuid;
    private Map<String, Integer> volumeChainToCheck;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public Map<String, Integer> getVolumeChainToCheck() {
        return volumeChainToCheck;
    }

    public void setVolumeChainToCheck(Map<String, Integer> volumeChainToCheck) {
        this.volumeChainToCheck = volumeChainToCheck;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }
}

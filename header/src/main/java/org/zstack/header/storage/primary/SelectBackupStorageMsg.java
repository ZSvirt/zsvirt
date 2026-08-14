package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by mingjian.deng on 2019/10/10.
 */
public class SelectBackupStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String volumeUuid;
    private long requiredSize;
    private List<String> requiredBackupStorageTypes;

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public long getRequiredSize() {
        return requiredSize;
    }

    public void setRequiredSize(long requiredSize) {
        this.requiredSize = requiredSize;
    }

    public List<String> getRequiredBackupStorageTypes() {
        return requiredBackupStorageTypes;
    }

    public void setRequiredBackupStorageTypes(List<String> requiredBackupStorageTypes) {
        this.requiredBackupStorageTypes = requiredBackupStorageTypes;
    }
}

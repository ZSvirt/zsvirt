package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

import java.util.List;

/**
 * Create by weiwang at 2018/7/2
 */
public class MigrateVolumesBetweenSharedBlockGroupPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String targetPrimaryStorageUuid;
    private String volumePath;
    private List<SharedBlockMigrateVolumeStruct> migrateVolumeStructs;

    public List<SharedBlockMigrateVolumeStruct> getMigrateVolumeStructs() {
        return migrateVolumeStructs;
    }

    public void setMigrateVolumeStructs(List<SharedBlockMigrateVolumeStruct> migrateVolumeStructs) {
        this.migrateVolumeStructs = migrateVolumeStructs;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getTargetPrimaryStorageUuid() {
        return targetPrimaryStorageUuid;
    }

    public void setTargetPrimaryStorageUuid(String targetPrimaryStorageUuid) {
        this.targetPrimaryStorageUuid = targetPrimaryStorageUuid;
    }

    public String getVolumePath() {
        return volumePath;
    }

    public void setVolumePath(String volumePath) {
        this.volumePath = volumePath;
    }
}

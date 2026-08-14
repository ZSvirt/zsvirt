package org.zstack.storage.primary.local;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

/**
 * Created by xing5 on 2016/7/21.
 */
public class MigrateVolumeOnLocalStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String volumeUuid;
    private String primaryStorageUuid;
    private String destHostUuid;
    private boolean keepOriginalVolumeInTrash;
    private boolean allocatedPrimaryStorageCapacity;

    public String getDestHostUuid() {
        return destHostUuid;
    }

    public void setDestHostUuid(String destHostUuid) {
        this.destHostUuid = destHostUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public boolean isKeepOriginalVolumeInTrash() {
        return keepOriginalVolumeInTrash;
    }

    public void setKeepOriginalVolumeInTrash(boolean keepOriginalVolumeInTrash) {
        this.keepOriginalVolumeInTrash = keepOriginalVolumeInTrash;
    }

    public boolean isAllocatedPrimaryStorageCapacity() {
        return allocatedPrimaryStorageCapacity;
    }

    public void setAllocatedPrimaryStorageCapacity(boolean allocatedPrimaryStorageCapacity) {
        this.allocatedPrimaryStorageCapacity = allocatedPrimaryStorageCapacity;
    }
}

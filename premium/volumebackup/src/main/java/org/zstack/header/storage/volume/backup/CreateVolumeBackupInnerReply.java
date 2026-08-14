package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.VolumeBackupInventory;

public class CreateVolumeBackupInnerReply extends MessageReply {
    private VolumeBackupInventory inventory;
    private Long actualExecuteTime;

    public VolumeBackupInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeBackupInventory inventory) {
        this.inventory = inventory;
    }

    public Long getActualExecuteTime() {
        return actualExecuteTime;
    }

    public void setActualExecuteTime(Long actualExecuteTime) {
        this.actualExecuteTime = actualExecuteTime;
    }
}

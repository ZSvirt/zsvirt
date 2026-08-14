package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.VolumeBackupInventory;

import java.util.List;

public class CreateVmBackupReply extends MessageReply {
    private List<VolumeBackupInventory> inventories;
    private Long actualExecuteTime;

    public List<VolumeBackupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeBackupInventory> inventories) {
        this.inventories = inventories;
    }

    public Long getActualExecuteTime() {
        return actualExecuteTime;
    }

    public void setActualExecuteTime(Long actualExecuteTime) {
        this.actualExecuteTime = actualExecuteTime;
    }
}


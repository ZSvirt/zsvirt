package org.zstack.header.storage.snapshot;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Create by weiwang at 2018/6/11
 */
public class CreateVolumesSnapshotOverlayInnerReply extends MessageReply {
    private List<VolumeSnapshotInventory> inventories;
    private List<String> hostBackupFileUuidList;

    public List<VolumeSnapshotInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeSnapshotInventory> inventories) {
        this.inventories = inventories;
    }

    public List<String> getHostBackupFileUuidList() {
        return hostBackupFileUuidList;
    }

    public void setHostBackupFileUuidList(List<String> hostBackupFileUuidList) {
        this.hostBackupFileUuidList = hostBackupFileUuidList;
    }
}

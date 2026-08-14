package org.zstack.sdk;

import org.zstack.sdk.VolumeSnapshotInventory;

public class SnapshotLeafInventory  {

    public VolumeSnapshotInventory inventory;
    public void setInventory(VolumeSnapshotInventory inventory) {
        this.inventory = inventory;
    }
    public VolumeSnapshotInventory getInventory() {
        return this.inventory;
    }

    public java.lang.String parentUuid;
    public void setParentUuid(java.lang.String parentUuid) {
        this.parentUuid = parentUuid;
    }
    public java.lang.String getParentUuid() {
        return this.parentUuid;
    }

    public java.util.List children;
    public void setChildren(java.util.List children) {
        this.children = children;
    }
    public java.util.List getChildren() {
        return this.children;
    }

}

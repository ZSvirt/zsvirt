package org.zstack.sdk;

import org.zstack.sdk.SharedBlockGroupType;

public class SharedBlockGroupPrimaryStorageInventory extends org.zstack.sdk.PrimaryStorageInventory {

    public java.util.List sharedBlocks;
    public void setSharedBlocks(java.util.List sharedBlocks) {
        this.sharedBlocks = sharedBlocks;
    }
    public java.util.List getSharedBlocks() {
        return this.sharedBlocks;
    }

    public SharedBlockGroupType sharedBlockGroupType;
    public void setSharedBlockGroupType(SharedBlockGroupType sharedBlockGroupType) {
        this.sharedBlockGroupType = sharedBlockGroupType;
    }
    public SharedBlockGroupType getSharedBlockGroupType() {
        return this.sharedBlockGroupType;
    }

}

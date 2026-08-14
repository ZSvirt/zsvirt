package org.zstack.storage.primary.sharedblock;

import org.zstack.header.storage.primary.RecalculatePrimaryStorageCapacityMsg;

public class SharedBlockRecalculatePrimaryStorageCapacityMsg extends RecalculatePrimaryStorageCapacityMsg {
    private boolean isRelease;

    public boolean isRelease() {
        return isRelease;
    }

    public void setRelease(boolean release) {
        isRelease = release;
    }
}

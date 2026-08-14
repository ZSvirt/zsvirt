package org.zstack.storage.primary.nfs;

import org.zstack.header.storage.primary.RecalculatePrimaryStorageCapacityMsg;

/**
 * Created by AlanJager on 2017/4/25.
 */
public class NfsRecalculatePrimaryStorageCapacityMsg extends RecalculatePrimaryStorageCapacityMsg {
    private boolean isRelease;

    public boolean isRelease() {
        return isRelease;
    }

    public void setRelease(boolean release) {
        isRelease = release;
    }
}

package org.zstack.expon.sdk.config;

import org.zstack.expon.sdk.ExponResponse;

public class SetTrashExpireTimeResponse extends ExponResponse {
    private int trashRecycle;

    public int getTrashRecycle() {
        return trashRecycle;
    }

    public void setTrashRecycle(int trashRecycle) {
        this.trashRecycle = trashRecycle;
    }
}

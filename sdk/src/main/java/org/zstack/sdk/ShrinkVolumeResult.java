package org.zstack.sdk;

import org.zstack.sdk.ShrinkResult;

public class ShrinkVolumeResult {
    public ShrinkResult shrinkResult;
    public void setShrinkResult(ShrinkResult shrinkResult) {
        this.shrinkResult = shrinkResult;
    }
    public ShrinkResult getShrinkResult() {
        return this.shrinkResult;
    }

}

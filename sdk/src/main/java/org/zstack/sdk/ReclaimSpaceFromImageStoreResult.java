package org.zstack.sdk;

import org.zstack.sdk.ImageStoreGcResult;

public class ReclaimSpaceFromImageStoreResult {
    public ImageStoreGcResult gcResult;
    public void setGcResult(ImageStoreGcResult gcResult) {
        this.gcResult = gcResult;
    }
    public ImageStoreGcResult getGcResult() {
        return this.gcResult;
    }

}

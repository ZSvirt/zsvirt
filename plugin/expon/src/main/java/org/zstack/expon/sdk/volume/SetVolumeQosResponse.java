package org.zstack.expon.sdk.volume;

import org.zstack.expon.sdk.ExponResponse;

public class SetVolumeQosResponse extends ExponResponse {
    private boolean result;

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }
}

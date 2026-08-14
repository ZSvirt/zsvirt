package org.zstack.expon.sdk.iscsi;

import org.zstack.expon.sdk.ExponResponse;

public class UnbindIscsiTargetFromUssResponse extends ExponResponse {
    private boolean result;

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }
}

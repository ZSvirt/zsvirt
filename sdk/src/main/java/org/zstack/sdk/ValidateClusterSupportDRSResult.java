package org.zstack.sdk;

import org.zstack.sdk.ErrorCode;

public class ValidateClusterSupportDRSResult {
    public boolean supported;
    public void setSupported(boolean supported) {
        this.supported = supported;
    }
    public boolean getSupported() {
        return this.supported;
    }

    public ErrorCode reason;
    public void setReason(ErrorCode reason) {
        this.reason = reason;
    }
    public ErrorCode getReason() {
        return this.reason;
    }

}

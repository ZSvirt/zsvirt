package org.zstack.sdk.keyprovider.nkp.api;

import org.zstack.sdk.NkpRestoreInfo;

public class ParseNkpRestoreResult {
    public NkpRestoreInfo restoreInfo;
    public void setRestoreInfo(NkpRestoreInfo restoreInfo) {
        this.restoreInfo = restoreInfo;
    }
    public NkpRestoreInfo getRestoreInfo() {
        return this.restoreInfo;
    }

    public java.lang.String code;
    public void setCode(java.lang.String code) {
        this.code = code;
    }
    public java.lang.String getCode() {
        return this.code;
    }

    public java.lang.String reason;
    public void setReason(java.lang.String reason) {
        this.reason = reason;
    }
    public java.lang.String getReason() {
        return this.reason;
    }

}

package org.zstack.expon.sdk;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.expon.ExponError;

import static org.zstack.core.Platform.operr;

public class ExponResponse {
    protected String retCode;
    protected String message;

    public String getRetCode() {
        return retCode;
    }

    public void setRetCode(String retCode) {
        this.retCode = retCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return "0".equals(retCode) || retCode == null;
    }

    public ErrorCode getError() {
        return operr(message);
    }

    public boolean sessionExpired() {
        return isError(ExponError.SESSION_EXPIRED, ExponError.SESSION_NOTFOUND, ExponError.INVALID_SESSION);
    }

    public boolean isError(ExponError... errorEnums) {
        for (ExponError e : errorEnums) {
            if (e.toString().equals(retCode)) {
                return true;
            }
        }

        return false;
    }
}

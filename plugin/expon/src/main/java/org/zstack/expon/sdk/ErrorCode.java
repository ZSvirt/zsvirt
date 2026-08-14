package org.zstack.expon.sdk;

import org.zstack.header.expon.ExponError;

public class ErrorCode {
    String retCode;
    String message;

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

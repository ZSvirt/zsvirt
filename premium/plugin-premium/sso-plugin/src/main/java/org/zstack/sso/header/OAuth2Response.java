package org.zstack.sso.header;

import org.zstack.header.errorcode.ErrorCode;

import java.util.Map;

/**
 * @Author: DaoDao
 * @Date: 2023/2/3
 */
public class OAuth2Response {
    private Map<String, Object> response;
    private ErrorCode errorCode;

    public Map<String, Object> getResponse() {
        return response;
    }

    public void setResponse(Map<String, Object> response) {
        this.response = response;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
}

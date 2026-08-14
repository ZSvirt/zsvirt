package org.zstack.pluginpremium.externalapiadapter.api;

import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by lining on 2018/5/8.
 */
public class APIError {

    private String Message;

    private String RequestId;

    private String HostId;

    private String Code;

    public String getMessage() {
        return Message;
    }

    public void setMessage(String message) {
        Message = message;
    }

    public String getRequestId() {
        return RequestId;
    }

    public void setRequestId(String requestId) {
        RequestId = requestId;
    }

    public String getHostId() {
        return HostId;
    }

    public void setHostId(String hostId) {
        HostId = hostId;
    }

    public String getCode() {
        return Code;
    }

    public void setCode(String code) {
        Code = code;
    }

    @Override
    public String toString() {
        return JSONObjectUtil.toJsonString(this);
    }
}

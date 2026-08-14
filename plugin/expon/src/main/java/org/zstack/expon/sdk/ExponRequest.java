package org.zstack.expon.sdk;

/**
 * Created by xing5 on 2016/12/9.
 */
public abstract class ExponRequest implements ExponParam {
    @Param
    String sessionId;

    long timeout = -1;

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}

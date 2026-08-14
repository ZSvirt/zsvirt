package org.zstack.loginControl.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/login/control/unlock",
        method = HttpMethod.GET,
        responseClass = APIUnlockIdentityReply.class
)
public class APIUnlockIdentityMsg extends APISyncCallMessage {
    @APIParam
    private String resourceName;
    @APIParam
    private String loginType;

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public static APIUnlockIdentityMsg __example__() {
        APIUnlockIdentityMsg msg = new APIUnlockIdentityMsg();
        msg.setResourceName("test-account");
        msg.setLoginType("ldap");
        return msg;
    }
}

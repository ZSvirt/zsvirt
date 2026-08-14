package org.zstack.header.identity.login;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/login/procedures",
        method = HttpMethod.GET,
        responseClass = APIGetLoginProceduresReply.class
)
@SuppressCredentialCheck
public class APIGetLoginProceduresMsg extends APISyncCallMessage {
    @APIParam
    private String username;
    /**
     * get procedures of specific login type
     */
    @APIParam
    private String loginType;

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public static APIGetLoginProceduresMsg __example__() {
        APIGetLoginProceduresMsg msg = new APIGetLoginProceduresMsg();
        msg.setUsername("admin");
        msg.setLoginType("iam1");
        return msg;
    }
}

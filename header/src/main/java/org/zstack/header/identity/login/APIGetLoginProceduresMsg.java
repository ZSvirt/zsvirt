package org.zstack.header.identity.login;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.AccountSource;
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
    /**
     * Optional account source used only to disambiguate accounts that share the
     * same name across different sources (ZSV-12379). Most callers should leave
     * it null; it is a selection hint, never an authorization basis.
     */
    @APIParam(required = false, validEnums = AccountSource.class)
    private String source;

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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public static APIGetLoginProceduresMsg __example__() {
        APIGetLoginProceduresMsg msg = new APIGetLoginProceduresMsg();
        msg.setUsername("admin");
        msg.setLoginType("iam1");
        return msg;
    }
}

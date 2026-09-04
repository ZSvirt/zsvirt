package org.zstack.loginControl.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 *  * Created by LiangHanYu on 2021/2/4 16:02
 *   */
@SuppressCredentialCheck
@RestRequest(
        path = "/password/verify",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIValidatePasswordReply.class
)
public class APIValidatePasswordMsg extends APISyncCallMessage {
    @APIParam
    private String loginName;
    @APIParam(password = true)
    @NoLogging
    private String password;
    @APIParam
    private String loginType;

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public static APIValidatePasswordMsg __example__() {
        APIValidatePasswordMsg msg = new APIValidatePasswordMsg();
        msg.setLoginName("test");
        msg.setPassword("password");
        msg.setLoginType(AccountConstant.LOGIN_TYPE);
        return msg;
    }
}

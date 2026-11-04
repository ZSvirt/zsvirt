package org.zstack.loginControl.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by kayo on 2018/7/10.
 */
@RestRequest(
        path = "/login/control/captcha",
        method = HttpMethod.GET,
        responseClass = APIGetLoginCaptchaReply.class
)
@SuppressCredentialCheck
public class APIGetLoginCaptchaMsg extends APISyncCallMessage {
    @APIParam
    private String resourceName;
    @APIParam
    private String loginType;
    @APIParam(required = false)
    private String captchaUuid;

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

    public String getCaptchaUuid() {
        return captchaUuid;
    }

    public void setCaptchaUuid(String captchaUuid) {
        this.captchaUuid = captchaUuid;
    }

    public static APIGetLoginCaptchaMsg __example__() {
        APIGetLoginCaptchaMsg msg = new APIGetLoginCaptchaMsg();
        msg.setLoginType("Test");
        msg.setResourceName("admin");
        return msg;
    }
}

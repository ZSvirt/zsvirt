package org.zstack.twoFactorAuthentication;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/twofactorauthentication/secret",
        method = HttpMethod.GET,
        responseClass = APIGetTwoFactorAuthenticationSecretReply.class
)
@SuppressCredentialCheck
public class APIGetTwoFactorAuthenticationSecretMsg extends APISyncCallMessage {
    @APIParam(nonempty = true)
    private String name;
    @APIParam(nonempty = true)
    @NoLogging
    private String password;
    @APIParam(required = false)
    private String captchaUuid;
    @APIParam(required = false)
    private String verifyCode;

    @APIParam(nonempty = true, validValues = {"account", "ldap"})
    private String type = TwoFactorAuthenticationConstant.LOGIN_TYPE_ACCOUNT;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCaptchaUuid() {
        return captchaUuid;
    }

    public void setCaptchaUuid(String captchaUuid) {
        this.captchaUuid = captchaUuid;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }

    public static APIGetTwoFactorAuthenticationSecretMsg __example__() {
        APIGetTwoFactorAuthenticationSecretMsg msg = new APIGetTwoFactorAuthenticationSecretMsg();

        msg.setName("user1");
        msg.setPassword("password");
        msg.setType("ldap");

        return msg;
    }
    
}
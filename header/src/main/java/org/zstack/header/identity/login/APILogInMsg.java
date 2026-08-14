package org.zstack.header.identity.login;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpMethod;
import org.zstack.header.identity.*;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APIReply;
import org.zstack.header.other.APILoginAuditor;
import org.zstack.header.rest.RestRequest;

import java.util.Map;

@SuppressCredentialCheck
@RestRequest(
        path = "/login",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APILogInReply.class
)
public class APILogInMsg extends APISessionMessage implements APILoginAuditor {
    @APIParam
    private String username;
    @APIParam
    @NoLogging
    private String password;
    @APIParam
    private String loginType;
    @APIParam(required = false)
    private String captchaUuid;
    @APIParam(required = false)
    private String verifyCode;
    @APIParam(required = false)
    private Map<String, String> clientInfo;
    @APIParam(required = false)
    private Map<String, String> properties;

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getLoginType() {
        return loginType;
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

    public Map<String, String> getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(Map<String, String> clientInfo) {
        this.clientInfo = clientInfo;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public LoginResult loginAudit(APIMessage msg, APIReply reply) {
        String clientIp = "";
        String clientBrowser = "";
        APILogInMsg amsg = (APILogInMsg) msg;
        Map<String, String> clientInfo = amsg.getClientInfo();
        if (clientInfo != null && !clientInfo.isEmpty()) {
            clientIp = StringUtils.isNotEmpty(clientInfo.get("clientIp")) ? clientInfo.get("clientIp") : "";
            clientBrowser = StringUtils.isNotEmpty(clientInfo.get("clientBrowser")) ? clientInfo.get("clientBrowser") : "";
        } else {
            clientIp = StringUtils.isNotBlank(msg.getClientIp()) ? msg.getClientIp() : "";
            clientBrowser = StringUtils.isNotBlank(msg.getClientBrowser()) ? msg.getClientBrowser() : "";
        }
        String resourceUuid = reply.isSuccess() ? ((APILogInReply) reply).getInventory().getUuid() : "";
        return new LoginResult(clientIp, clientBrowser, resourceUuid, SessionVO.class);
    }

    @Override
    public String getOperator() {
        return username;
    }

    public static APILogInMsg __example__() {
        APILogInMsg msg = new APILogInMsg();
        msg.setUsername("admin");
        msg.setPassword("password");
        msg.setLoginType("account");
        return msg;
    }
}

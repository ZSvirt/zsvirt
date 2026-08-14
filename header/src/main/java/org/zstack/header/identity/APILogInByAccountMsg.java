package org.zstack.header.identity;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpMethod;
import org.zstack.header.identity.login.APICaptchaMessage;
import org.zstack.header.core.encrypt.EncryptionParamAllowed;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APIReply;
import org.zstack.header.other.APILoginAuditor;
import org.zstack.header.rest.RestRequest;

import java.util.Map;

@SuppressCredentialCheck
@RestRequest(
        path = "/accounts/login",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APILogInReply.class
)
@EncryptionParamAllowed(actions = { EncryptionParamAllowed.ACTION_PUT_USER_INFO_INTO_SYSTEM_TAG })
public class APILogInByAccountMsg extends APISessionMessage implements APILoginAuditor, APICaptchaMessage {
    @APIParam
    private String accountName;
    @APIParam(password = true)
    @NoLogging
    private String password;
    @APIParam(required = false)
    private String accountType;
    @APIParam(required = false)
    private String captchaUuid;
    @APIParam(required = false)
    private String verifyCode;
    @APIParam(required = false)
    private Map<String, String> clientInfo;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
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

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public Map<String, String> getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(Map<String, String> clientInfo) {
        this.clientInfo = clientInfo;
    }

    @Override
    public String getOperator() {
        return accountName;
    }

    public static APILogInByAccountMsg __example__() {
        APILogInByAccountMsg msg = new APILogInByAccountMsg();
        msg.setAccountName("test");
        msg.setPassword("password");
        msg.setCaptchaUuid("39bd748906ad301793c64f688dc197a9");
        msg.setVerifyCode("test");
        return msg;
    }

    @Override
    public LoginResult loginAudit(APIMessage msg, APIReply reply) {
        String clientIp = "";
        String clientBrowser = "";
        APILogInByAccountMsg amsg = (APILogInByAccountMsg) msg;
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
    public String getUsername() {
        return accountName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getLoginType() {
        return AccountConstant.LOGIN_TYPE;
    }
}

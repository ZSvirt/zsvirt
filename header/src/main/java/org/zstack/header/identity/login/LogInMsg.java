package org.zstack.header.identity.login;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.NeedReplyMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogInMsg extends NeedReplyMessage {
    private String username;
    @NoLogging
    private String password;
    private String loginType;
    private SessionInventory session;
    private String captchaUuid;
    private String verifyCode;
    private Map<String, String> clientInfo;
    private boolean validateOnly = false;
    private List<AdditionalAuthFeature> ignoreAdditionalFeatures;
    private Map<String, String> properties = new HashMap<>();

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
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

    public boolean isValidateOnly() {
        return validateOnly;
    }

    public void setValidateOnly(boolean validateOnly) {
        this.validateOnly = validateOnly;
    }

    public List<AdditionalAuthFeature> getIgnoreAdditionalFeatures() {
        return ignoreAdditionalFeatures;
    }

    public void setIgnoreAdditionalFeatures(List<AdditionalAuthFeature> ignoreAdditionalFeatures) {
        this.ignoreAdditionalFeatures = ignoreAdditionalFeatures;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}

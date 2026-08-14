package org.zstack.sso.cas.header;

import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.entity.SyncDeletedAccountStrategy;
import org.zstack.identity.imports.header.AbstractAccountSourceSpec;

public class CasAccountSourceSpec extends AbstractAccountSourceSpec {
    private String name;
    private String loginMNUrl;
    private String redirectUrl;
    private String casServerLoginUrl;
    private String casServerUrlPrefix;
    private String serverName;
    private SyncCreatedAccountStrategy createdAccountStrategy = SyncCreatedAccountStrategy.CreateAccount;
    private SyncDeletedAccountStrategy deleteAccountStrategy = SyncDeletedAccountStrategy.NoAction;
    private String urlTemplate;
    private String usernameProperty;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLoginMNUrl() {
        return loginMNUrl;
    }

    public void setLoginMNUrl(String loginMNUrl) {
        this.loginMNUrl = loginMNUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getCasServerLoginUrl() {
        return casServerLoginUrl;
    }

    public void setCasServerLoginUrl(String casServerLoginUrl) {
        this.casServerLoginUrl = casServerLoginUrl;
    }

    public String getCasServerUrlPrefix() {
        return casServerUrlPrefix;
    }

    public void setCasServerUrlPrefix(String casServerUrlPrefix) {
        this.casServerUrlPrefix = casServerUrlPrefix;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public SyncCreatedAccountStrategy getCreatedAccountStrategy() {
        return createdAccountStrategy;
    }

    public void setCreatedAccountStrategy(SyncCreatedAccountStrategy createdAccountStrategy) {
        this.createdAccountStrategy = createdAccountStrategy;
    }

    public SyncDeletedAccountStrategy getDeleteAccountStrategy() {
        return deleteAccountStrategy;
    }

    public void setDeleteAccountStrategy(SyncDeletedAccountStrategy deleteAccountStrategy) {
        this.deleteAccountStrategy = deleteAccountStrategy;
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public String getUsernameProperty() {
        return usernameProperty;
    }

    public void setUsernameProperty(String usernameProperty) {
        this.usernameProperty = usernameProperty;
    }
}
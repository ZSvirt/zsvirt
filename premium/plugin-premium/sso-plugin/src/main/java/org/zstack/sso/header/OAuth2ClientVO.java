package org.zstack.sso.header;

import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * @Author: DaoDao
 * @Date: 2022/8/23
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
public class OAuth2ClientVO extends ThirdPartyAccountSourceVO {
    @Column
    private String clientId;
    @Column
    private String clientSecret;
    @Column
    private String grantType;
    @Column
    private String loginMNUrl;
    @Column
    private String redirectUrl;
    @Column
    private String authorizationUrl;
    @Column
    private String tokenUrl;
    @Column
    private String userinfoUrl;
    @Column
    private String logoutUrl;
    @Column
    private String usernameProperty;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
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

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getUserinfoUrl() {
        return userinfoUrl;
    }

    public void setUserinfoUrl(String userinfoUrl) {
        this.userinfoUrl = userinfoUrl;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public void setLogoutUrl(String logoutUrl) {
        this.logoutUrl = logoutUrl;
    }

    public String getUsernameProperty() {
        return usernameProperty;
    }

    public void setUsernameProperty(String usernameProperty) {
        this.usernameProperty = usernameProperty;
    }
}

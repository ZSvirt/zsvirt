package org.zstack.sso.header;

import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * @Author: DaoDao
 * @Date: 2022/8/24
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
public class CasClientVO extends ThirdPartyAccountSourceVO {
    @Column
    private String loginMNUrl;
    @Column
    private String redirectUrl;
    @Column
    private String casServerLoginUrl;
    @Column
    private String casServerUrlPrefix;
    @Column
    private String serverName;
    @Column
    @Enumerated(EnumType.STRING)
    private CasState state;
    @Column
    private String usernameProperty;

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

    public CasState getState() {
        return state;
    }

    public void setState(CasState state) {
        this.state = state;
    }

    public String getUsernameProperty() {
        return usernameProperty;
    }

    public void setUsernameProperty(String usernameProperty) {
        this.usernameProperty = usernameProperty;
    }
}

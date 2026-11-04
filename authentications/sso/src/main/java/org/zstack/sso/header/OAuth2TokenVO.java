package org.zstack.sso.header;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * @Author: DaoDao
 * @Date: 2023/2/3
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
public class OAuth2TokenVO extends SSOTokenVO {
    @Column
    private String accessToken;

    @Column
    private String idToken;

    @Column
    private String refreshToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public OAuth2TokenVO() {
    }

    public OAuth2TokenVO(SSOTokenVO vo) {
        super(vo);
    }
}

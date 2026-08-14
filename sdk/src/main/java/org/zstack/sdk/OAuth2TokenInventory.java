package org.zstack.sdk;



public class OAuth2TokenInventory extends org.zstack.sdk.SSOTokenInventory {

    public java.lang.String accessToken;
    public void setAccessToken(java.lang.String accessToken) {
        this.accessToken = accessToken;
    }
    public java.lang.String getAccessToken() {
        return this.accessToken;
    }

    public java.lang.String idToken;
    public void setIdToken(java.lang.String idToken) {
        this.idToken = idToken;
    }
    public java.lang.String getIdToken() {
        return this.idToken;
    }

    public java.lang.String refreshToken;
    public void setRefreshToken(java.lang.String refreshToken) {
        this.refreshToken = refreshToken;
    }
    public java.lang.String getRefreshToken() {
        return this.refreshToken;
    }

}

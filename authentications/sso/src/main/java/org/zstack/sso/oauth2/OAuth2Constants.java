package org.zstack.sso.oauth2;

/**
 * @Author: DaoDao
 * @Date: 2022/8/24
 */
public interface OAuth2Constants {
    //default use OIDC,
    public static final String AUTH_SCOPE_VALUE= "openid";

    public static final int AUTH_RESPONSE_SUCCESS = 200;
    public static final int AUTH_RESPONSE_ERROR = 500;

    public static final String OAUTH2_REDIRECT = "/sso/oauth2";
    public static final String TOKEN_INTROSPECT_PATH= "/introspect";
    public static final String TOKEN_Authorization = "Authorization";
    public static final String OAUTH2_TOKEN = "token";
    public static final String TOKEN_INTROSPECT_ACTIVE = "active";
    public static final String GRANT_TYPE_NAME = "grant_type";
    public static final String GRANT_TYPE_OCDE_VALUE = "authorization_code";
    public static final String REFRESH_TOKEN_VALUE = "refresh_token";
    public static final String GRANT_TYPE_PASSWORD_VALUE = "password";
    public static final String AUTH_REDIRECT_URL_NAME= "redirect_uri";
    public static final String CLIENT_ID_NAME = "client_id";
    public static final String CLIENT_SECRET_NAME = "client_secret";
    public static final String OAUTH2_CODE = "code";
    public static final String AUTH_SCOPE_NAME= "scope";
    public static final String RESPONSE_TYPE_NAME = "response_type";
    public static final String AUTH2_STATE = "state";
    public static final String KEY_SID = "sid";
    public static final String KEY_SUB = "sub";

    public static final String OAUTH2_USER_NAME = "username";
    public static final String OAUTH2_PASSWORD = "password";

    public static final String OIDC_GET_USERNAME = "preferred_username";
    public static final String IAM2_ORGANIZATIONS = "organization";

    public static final String OIDC_SSO_CLIENT_TYPE = "OIDC";
    public static final String OAUTH2_SSO_CLIENT_TYPE = "OAuth2";

    public static final String ID_TOKEN_VALUE = "id_token";
    public static final String ACCESS_TOKEN_VALUE = "access_token";
    public static final String TOKEN_EXPIRE_TIME_NAME = "exp";

}

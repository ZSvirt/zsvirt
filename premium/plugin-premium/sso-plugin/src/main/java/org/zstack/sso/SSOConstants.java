package org.zstack.sso;

/**
 * @Author: DaoDao
 * @Date: 2022/8/24
 */
public interface SSOConstants {
    public static final String SERVICE_ID = "sso";
    public static final String CAS_CLIENT_TYPE = "CAS";
    public static final String OAUTH2_CLIENT_TYPE = "OAuth2";
    public static final String OAUTH2_LOGIN_URL = "http://%s/zstack/sso/oauth2?clientUuid=%s";
    public static final String CAS_LOGIN_URL = "http://%s:%s/zstack/sso/cas?clientUuid=%s";

    public static final String CAS_SERVER_LOGINURL_NAME = "casServerLoginUrl";
    public static final String CAS_SERVER_URLPREFIX_NAME = "casServerUrlPrefix";
    public static final String CAS_SERVER_NAME = "serverName";
    public static final String USER_NAME = "name";
    public static final String IAM2_VIRTUAL_TYPE = "CAS";
    public static final String CLIENT_UUID_NAME = "clientUuid";
    public static final String REDIRECT_NAME = "redirect";
    public static final String PROJECTUUID_NAME = "projectUuid";
    public static final String ZONEUUID_NAME = "zoneUuid";
    public static final String URL_TEMPLATE = "default";

}

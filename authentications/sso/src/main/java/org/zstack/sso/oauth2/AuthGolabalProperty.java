package org.zstack.sso.oauth2;

/**
 * @Author: DaoDao
 * @Date: 2022/8/25
 */
import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class AuthGolabalProperty {
    @GlobalProperty(name="oauth2.token.userinfo", defaultValue = "access_token")
    public static String OAUTH2_GET_TOKEN_USERINFO;
    @GlobalProperty(name = "oidc.get.username", defaultValue = "preferred_username")
    public static String OIDC_ACCOUNT_NAME;
}

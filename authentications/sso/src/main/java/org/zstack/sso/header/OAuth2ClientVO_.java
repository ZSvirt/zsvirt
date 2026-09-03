package org.zstack.sso.header;

import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * @Author: DaoDao
 * @Date: 2022/8/23
 */
@StaticMetamodel(OAuth2ClientVO.class)
public class OAuth2ClientVO_ extends ThirdPartyAccountSourceVO_ {
    public static volatile SingularAttribute<OAuth2ClientVO, String> clientId;
    public static volatile SingularAttribute<OAuth2ClientVO, String> clientSecret;
    public static volatile SingularAttribute<OAuth2ClientVO, String> grantType;
    public static volatile SingularAttribute<OAuth2ClientVO, String> loginMNUrl;
    public static volatile SingularAttribute<OAuth2ClientVO, String> redirectUrl;
    public static volatile SingularAttribute<OAuth2ClientVO, String> authorizationUrl;
    public static volatile SingularAttribute<OAuth2ClientVO, String> tokenUrl;
    public static volatile SingularAttribute<OAuth2ClientVO, String> userinfoUrl;
    public static volatile SingularAttribute<OAuth2ClientVO, String> logoutUrl;
    public static volatile SingularAttribute<OAuth2ClientVO, String> usernameProperty;
}

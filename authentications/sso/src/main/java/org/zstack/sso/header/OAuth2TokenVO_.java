package org.zstack.sso.header;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * @Author: DaoDao
 * @Date: 2023/2/3
 */
@StaticMetamodel(OAuth2TokenVO.class)
public class OAuth2TokenVO_ extends SSOTokenVO_ {
    public static volatile SingularAttribute<OAuth2TokenVO, String> accessToken;
    public static volatile SingularAttribute<OAuth2TokenVO, String> idToken;
    public static volatile SingularAttribute<OAuth2TokenVO, String> refreshToken;
}

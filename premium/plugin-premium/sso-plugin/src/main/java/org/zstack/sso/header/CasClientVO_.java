package org.zstack.sso.header;

import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * @Author: DaoDao
 * @Date: 2022/8/24
 */
@StaticMetamodel(CasClientVO.class)
public class CasClientVO_ extends ThirdPartyAccountSourceVO_ {
    public static volatile SingularAttribute<CasClientVO, String> loginMNUrl;
    public static volatile SingularAttribute<CasClientVO, String> redirectUrl;
    public static volatile SingularAttribute<CasClientVO, String> casServerLoginUrl;
    public static volatile SingularAttribute<CasClientVO, String> casServerUrlPrefix;
    public static volatile SingularAttribute<CasClientVO, String> serverName;
    public static volatile SingularAttribute<CasClientVO, CasState> state;
    public static volatile SingularAttribute<CasClientVO, String> usernameProperty;
}

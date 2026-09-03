package org.zstack.sso.header;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * @Author: DaoDao
 * @Date: 2022/9/6
 */
@StaticMetamodel(SSORedirectTemplateVO.class)
public class SSORedirectTemplateVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SSORedirectTemplateVO, String> name;
    public static volatile SingularAttribute<SSORedirectTemplateVO, String> description;
    public static volatile SingularAttribute<SSORedirectTemplateVO, String> clientUuid;
    public static volatile SingularAttribute<SSORedirectTemplateVO, String> redirectTemplate;
    public static volatile SingularAttribute<SSORedirectTemplateVO, Timestamp> createDate;
    public static volatile SingularAttribute<SSORedirectTemplateVO, Timestamp> lastOpDate;

}

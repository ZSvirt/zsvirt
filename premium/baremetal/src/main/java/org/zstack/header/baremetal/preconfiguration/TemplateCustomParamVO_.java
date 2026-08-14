package org.zstack.header.baremetal.preconfiguration;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by GuoYi on 2018-12-28.
 */
@StaticMetamodel(TemplateCustomParamVO.class)
public class TemplateCustomParamVO_ {
    public static volatile SingularAttribute<TemplateCustomParamVO, Long> id;
    public static volatile SingularAttribute<TemplateCustomParamVO, String> templateUuid;
    public static volatile SingularAttribute<TemplateCustomParamVO, String> param;
}

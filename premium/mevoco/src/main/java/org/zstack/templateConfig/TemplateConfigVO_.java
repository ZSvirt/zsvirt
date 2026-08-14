package org.zstack.templateConfig;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(TemplateConfigVO.class)
public class TemplateConfigVO_ {
    public static volatile SingularAttribute<TemplateConfigVO, Long> id;
    public static volatile SingularAttribute<TemplateConfigVO, String> name;
    public static volatile SingularAttribute<TemplateConfigVO, String> category;
    public static volatile SingularAttribute<TemplateConfigVO, String> templateUuid;
    public static volatile SingularAttribute<TemplateConfigVO, String> defaultValue;
    public static volatile SingularAttribute<TemplateConfigVO, String> value;
}

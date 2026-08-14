package org.zstack.templateConfig;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(GlobalConfigTemplateVO.class)
public class GlobalConfigTemplateVO_ {
    public static volatile SingularAttribute<GlobalConfigTemplateVO, String> uuid;
    public static volatile SingularAttribute<GlobalConfigTemplateVO, String> name;
    public static volatile SingularAttribute<GlobalConfigTemplateVO, String> type;
    public static volatile SingularAttribute<GlobalConfigTemplateVO, String> description;
}
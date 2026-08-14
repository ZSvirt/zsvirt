package org.zstack.core.config;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(GlobalConfigVO.class)
public class GlobalConfigVO_ {
	public static volatile SingularAttribute<GlobalConfigVO, Long> id;
    public static volatile SingularAttribute<GlobalConfigVO, String> name;
    public static volatile SingularAttribute<GlobalConfigVO, String> description;
    public static volatile SingularAttribute<GlobalConfigVO, String> category;
    public static volatile SingularAttribute<GlobalConfigVO, String> defaultValue;
    public static volatile SingularAttribute<GlobalConfigVO, String> value;
}

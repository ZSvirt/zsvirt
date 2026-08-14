package org.zstack.resourceconfig;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ResourceConfigVO.class)
public class ResourceConfigVO_ {
    public static volatile SingularAttribute<ResourceConfigVO, String> uuid;
    public static volatile SingularAttribute<ResourceConfigVO, String> name;
    public static volatile SingularAttribute<ResourceConfigVO, String> category;
    public static volatile SingularAttribute<ResourceConfigVO, String> resourceType;
    public static volatile SingularAttribute<ResourceConfigVO, String> resourceUuid;
    public static volatile SingularAttribute<ResourceConfigVO, String> value;
    public static volatile SingularAttribute<ResourceConfigVO, String> description;
    public static volatile SingularAttribute<ResourceConfigVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<ResourceConfigVO, Timestamp> createDate;
}

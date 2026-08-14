package org.zstack.header.resourceattribute.entity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ResourceAttributeValueVO.class)
public class ResourceAttributeValueVO_ {
    public static volatile SingularAttribute<ResourceAttributeValueVO, Long> id;
    public static volatile SingularAttribute<ResourceAttributeValueVO, String> keyUuid;
    public static volatile SingularAttribute<ResourceAttributeValueVO, String> value;
    public static volatile SingularAttribute<ResourceAttributeValueVO, String> resourceUuid;
    public static volatile SingularAttribute<ResourceAttributeValueVO, String> resourceType;
    public static volatile SingularAttribute<ResourceAttributeValueVO, Timestamp> createDate;
}

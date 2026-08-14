package org.zstack.header.resourceattribute.entity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ResourceAttributeKeyResourceTypeVO.class)
public class ResourceAttributeKeyResourceTypeVO_ {
    public static volatile SingularAttribute<ResourceAttributeKeyResourceTypeVO, Long> id;
    public static volatile SingularAttribute<ResourceAttributeKeyResourceTypeVO, String> keyUuid;
    public static volatile SingularAttribute<ResourceAttributeKeyResourceTypeVO, String> resourceType;
    public static volatile SingularAttribute<ResourceAttributeKeyResourceTypeVO, Timestamp> createDate;
}

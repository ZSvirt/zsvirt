package org.zstack.header.resourceattribute.entity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ResourceAttributeConstraintVO.class)
public class ResourceAttributeConstraintVO_ {
    public static volatile SingularAttribute<ResourceAttributeConstraintVO, Long> id;
    public static volatile SingularAttribute<ResourceAttributeConstraintVO, String> keyUuid;
    public static volatile SingularAttribute<ResourceAttributeConstraintVO, String> type;
    public static volatile SingularAttribute<ResourceAttributeConstraintVO, String> parameter;
    public static volatile SingularAttribute<ResourceAttributeConstraintVO, Timestamp> createDate;
    public static volatile SingularAttribute<ResourceAttributeConstraintVO, Timestamp> lastOpDate;
}

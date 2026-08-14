package org.zstack.header.resourceattribute.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ResourceAttributeKeyVO.class)
public class ResourceAttributeKeyVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ResourceAttributeKeyVO, String> name;
    public static volatile SingularAttribute<ResourceAttributeKeyVO, String> description;
    public static volatile SingularAttribute<ResourceAttributeKeyVO, Timestamp> createDate;
    public static volatile SingularAttribute<ResourceAttributeKeyVO, Timestamp> lastOpDate;
}

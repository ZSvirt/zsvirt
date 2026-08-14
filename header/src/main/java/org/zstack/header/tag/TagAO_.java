package org.zstack.header.tag;

/**
 */

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(TagAO.class)
public class TagAO_ {
    public static volatile SingularAttribute<TagAO, String> uuid;
    public static volatile SingularAttribute<TagAO, String> resourceUuid;
    public static volatile SingularAttribute<TagAO, String> resourceType;
    public static volatile SingularAttribute<TagAO, String> tag;
    public static volatile SingularAttribute<TagAO, TagType> type;
    public static volatile SingularAttribute<TagAO, Timestamp> createDate;
    public static volatile SingularAttribute<TagAO, Timestamp> lastOpDate;
}

package org.zstack.header.cbt;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(CbtTaskResourceRefVO.class)
public class CbtTaskResourceRefVO_ {
    public static volatile SingularAttribute<CbtTaskResourceRefVO, Long> id;
    public static volatile SingularAttribute<CbtTaskResourceRefVO, String> taskUuid;
    public static volatile SingularAttribute<CbtTaskResourceRefVO, String> resourceUuid;
    public static volatile SingularAttribute<CbtTaskResourceRefVO, String> resourceType;
    public static volatile SingularAttribute<CbtTaskResourceRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<CbtTaskResourceRefVO, Timestamp> lastOpDate;
}

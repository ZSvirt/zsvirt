package org.zstack.header.cbt;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(CbtTaskVO.class)
public class CbtTaskVO_ {
    public static volatile SingularAttribute<CbtTaskVO, String> uuid;
    public static volatile SingularAttribute<CbtTaskVO, String> name;
    public static volatile SingularAttribute<CbtTaskVO, String> description;
    public static volatile SingularAttribute<CbtTaskVO, CbtTaskStatus> status;
    public static volatile SingularAttribute<CbtTaskVO, Timestamp> createDate;
    public static volatile SingularAttribute<CbtTaskVO, Timestamp> lastOpDate;
}

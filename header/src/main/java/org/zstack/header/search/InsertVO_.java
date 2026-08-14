package org.zstack.header.search;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.util.Date;

@StaticMetamodel(InsertVO.class)
public class InsertVO_ {
    public static volatile SingularAttribute<InsertVO, Long> id;
    public static volatile SingularAttribute<InsertVO, String> voName;
    public static volatile SingularAttribute<InsertVO, String> uuid;
    public static volatile SingularAttribute<InsertVO, String> foreignVOUuid;
    public static volatile SingularAttribute<InsertVO, String> foreignVOName;
    public static volatile SingularAttribute<InsertVO, Date> insertDate;
}

package org.zstack.header.search;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.util.Date;

@StaticMetamodel(UpdateVO.class)
public class UpdateVO_ {
    public static volatile SingularAttribute<UpdateVO, Long> id;
    public static volatile SingularAttribute<UpdateVO, String> voName;
    public static volatile SingularAttribute<UpdateVO, String> uuid;
    public static volatile SingularAttribute<UpdateVO, String> foreignVOUuid;
    public static volatile SingularAttribute<UpdateVO, String> foreignVOName;
    public static volatile SingularAttribute<UpdateVO, Date> updateDate;
}

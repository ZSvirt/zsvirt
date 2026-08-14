package org.zstack.core.checkpoint;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.util.Date;

@StaticMetamodel(CheckPointEntryVO.class)
public class CheckPointVO_ {
    public static volatile SingularAttribute<CheckPointVO, Long> id;
    public static volatile SingularAttribute<CheckPointVO, String> name;
    public static volatile SingularAttribute<CheckPointVO, String> uuid;
    public static volatile SingularAttribute<CheckPointVO, CheckPointState> state;
    public static volatile SingularAttribute<CheckPointVO, Date> opDate;
    public static volatile SingularAttribute<CheckPointVO, Byte[]> context;
}

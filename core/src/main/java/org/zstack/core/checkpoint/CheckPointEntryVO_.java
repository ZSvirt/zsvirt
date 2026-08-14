package org.zstack.core.checkpoint;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(CheckPointEntryVO.class)
public class CheckPointEntryVO_ {
    public static volatile SingularAttribute<CheckPointEntryVO, Long> id;
    public static volatile SingularAttribute<CheckPointEntryVO, Long> checkPointId;
    public static volatile SingularAttribute<CheckPointEntryVO, String> name;
    public static volatile SingularAttribute<CheckPointEntryVO, CheckPointState> state;
    public static volatile SingularAttribute<CheckPointEntryVO, String> reason;
    public static volatile SingularAttribute<CheckPointVO, Byte[]> context;
}

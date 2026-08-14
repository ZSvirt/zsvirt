package org.zstack.header.vm;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by Wenhao.Zhang on 21/06/22
 */
@StaticMetamodel(VmCrashHistoryVO.class)
public class VmCrashHistoryVO_ {
    public static volatile SingularAttribute<VmCrashHistoryVO, Long> id;
    public static volatile SingularAttribute<VmCrashHistoryVO, String> uuid;
    public static volatile SingularAttribute<VmCrashHistoryVO, Long> dateInLong;
    public static volatile SingularAttribute<VmCrashHistoryVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmCrashHistoryVO, Timestamp> lastOpDate;
}

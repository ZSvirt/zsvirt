package org.zstack.core.gc;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by frank on 8/5/2015.
 */
@StaticMetamodel(GarbageCollectorVO.class)
public class GarbageCollectorVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<GarbageCollectorVO, String> name;
    public static volatile SingularAttribute<GarbageCollectorVO, String> runnerClass;
    public static volatile SingularAttribute<GarbageCollectorVO, String> context;
    public static volatile SingularAttribute<GarbageCollectorVO, GCStatus> status;
    public static volatile SingularAttribute<GarbageCollectorVO, String> type;
    public static volatile SingularAttribute<GarbageCollectorVO, String> managementNodeUuid;
    public static volatile SingularAttribute<GarbageCollectorVO, Timestamp> createDate;
    public static volatile SingularAttribute<GarbageCollectorVO, Timestamp> lastOpDate;
}

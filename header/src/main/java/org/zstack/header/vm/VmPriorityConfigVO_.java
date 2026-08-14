package org.zstack.header.vm;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmPriorityConfigVO.class)
public class VmPriorityConfigVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VmPriorityConfigVO, VmPriorityLevel> level;
    public static volatile SingularAttribute<VmPriorityConfigVO, String> accountUuid;
    public static volatile SingularAttribute<VmPriorityConfigVO, Integer> cpuShares;
    public static volatile SingularAttribute<VmPriorityConfigVO, Integer> oomScoreAdj;
    public static volatile SingularAttribute<VmPriorityConfigVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmPriorityConfigVO, Timestamp> lastOpDate;
}

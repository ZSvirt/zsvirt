package org.zstack.header.host;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HostPhysicalCpuVO.class)
public class HostPhysicalCpuVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<HostPhysicalCpuVO, String> hostUuid;
    public static volatile SingularAttribute<HostPhysicalCpuVO, String> socketDesignation;
    public static volatile SingularAttribute<HostPhysicalCpuVO, String> version;
    public static volatile SingularAttribute<HostPhysicalCpuVO, String> currentSpeed;
    public static volatile SingularAttribute<HostPhysicalCpuVO, Integer> coreCount;
    public static volatile SingularAttribute<HostPhysicalCpuVO, Integer> threadCount;
    public static volatile SingularAttribute<HostPhysicalCpuVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostPhysicalCpuVO, Timestamp> lastOpDate;
}

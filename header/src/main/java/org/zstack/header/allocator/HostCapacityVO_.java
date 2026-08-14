package org.zstack.header.allocator;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(HostCapacityVO.class)
public class HostCapacityVO_ {
    public static volatile SingularAttribute<HostCapacityVO, String> uuid;
    public static volatile SingularAttribute<HostCapacityVO, Long> totalMemory;
    public static volatile SingularAttribute<HostCapacityVO, Long> totalCpu;
    public static volatile SingularAttribute<HostCapacityVO, Integer> cpuNum;
    public static volatile SingularAttribute<HostCapacityVO, Integer> cpuSockets;
    public static volatile SingularAttribute<HostCapacityVO, Integer> cpuCoreNum;
    public static volatile SingularAttribute<HostCapacityVO, Long> availableMemory;
    public static volatile SingularAttribute<HostCapacityVO, Long> availableCpu;
    public static volatile SingularAttribute<HostCapacityVO, Long> totalPhysicalMemory;
    public static volatile SingularAttribute<HostCapacityVO, Long> availablePhysicalMemory;
}

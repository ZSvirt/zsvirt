package org.zstack.header.host;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/28 10:57
 */
@StaticMetamodel(HostHwMonitorStatusVO.class)
public class HostHwMonitorStatusVO_ {

    public static volatile SingularAttribute<HostHwMonitorStatusVO, String> uuid;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, HwMonitorStatus> cpuStatus;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, HwMonitorStatus> memoryStatus;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, HwMonitorStatus> diskStatus;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, HwMonitorStatus> nicStatus;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, HwMonitorStatus> gpuStatus;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, HwMonitorStatus> powerSupplyStatus;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, HwMonitorStatus> fanStatus;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, HwMonitorStatus> raidStatus;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, HwMonitorStatus> temperatureStatus;
}

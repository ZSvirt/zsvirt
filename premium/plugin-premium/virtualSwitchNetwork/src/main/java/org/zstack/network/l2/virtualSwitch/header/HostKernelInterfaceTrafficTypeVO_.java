package org.zstack.network.l2.virtualSwitch.header;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HostKernelInterfaceTrafficTypeVO.class)
public class HostKernelInterfaceTrafficTypeVO_ {
    public static volatile SingularAttribute<HostKernelInterfaceTrafficTypeVO, Long> id;
    public static volatile SingularAttribute<HostKernelInterfaceTrafficTypeVO, String> hostKernelInterfaceUuid;
    public static volatile SingularAttribute<HostKernelInterfaceTrafficTypeVO, HostKernelInterfaceTrafficType> trafficType;
    public static volatile SingularAttribute<HostKernelInterfaceTrafficTypeVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostKernelInterfaceTrafficTypeVO, Timestamp> lastOpDate;
}

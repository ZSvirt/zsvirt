package org.zstack.network.l2.virtualSwitch.header;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import org.zstack.header.network.l3.UsedIpVO_;

@StaticMetamodel(HostKernelInterfaceUsedIpVO.class)
public class HostKernelInterfaceUsedIpVO_ extends UsedIpVO_ {
    public static volatile SingularAttribute<HostKernelInterfaceUsedIpVO, String> hostKernelInterfaceUuid;
}

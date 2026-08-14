package org.zstack.header.host;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HostNetworkInterfaceServiceRefVO.class)
public class HostNetworkInterfaceServiceRefVO_ {
    public static volatile SingularAttribute<HostNetworkInterfaceServiceRefVO, Integer> id;
    public static volatile SingularAttribute<HostNetworkInterfaceServiceRefVO, String> interfaceUuid;
    public static volatile SingularAttribute<HostNetworkInterfaceServiceRefVO, Integer> vlanId;
    public static volatile SingularAttribute<HostNetworkInterfaceServiceRefVO, HostNetworkInterfaceServiceType> serviceType;
    public static volatile SingularAttribute<HostNetworkInterfaceServiceRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostNetworkInterfaceServiceRefVO, Timestamp> lastOpDate;
}


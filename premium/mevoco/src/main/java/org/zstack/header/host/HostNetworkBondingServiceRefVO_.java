package org.zstack.header.host;

import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HostNetworkBondingServiceRefVO.class)
public class HostNetworkBondingServiceRefVO_ {
    public static volatile SingularAttribute<HostNetworkInterfaceServiceRefVO, Integer> id;
    public static volatile SingularAttribute<HostNetworkBondingVO, String> bondingUuid;
    public static volatile SingularAttribute<HostNetworkBondingVO, Integer> vlanId;
    public static volatile SingularAttribute<HostNetworkBondingVO, HostNetworkInterfaceServiceType> serviceType;
    public static volatile SingularAttribute<HostNetworkBondingVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostNetworkBondingVO, Timestamp> lastOpDate;
}


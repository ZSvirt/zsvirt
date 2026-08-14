package org.zstack.network.hostNetworkInterface.lldp.entity;


import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HostNetworkInterfaceLldpRefVO.class)
public class HostNetworkInterfaceLldpRefVO_ {
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, String> lldpUuid;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, String> chassisId;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, Integer> timeToLive;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, String> managementAddress;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, String> systemName;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, String> systemDescription;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, String> systemCapabilities;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, String> portDescription;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, Integer> vlanId;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, Long> aggregationPortId;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, Integer> mtu;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, Timestamp> lastOpDate;
}

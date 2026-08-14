package org.zstack.header.vm;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmGuestNetworkInfoVO.class)
public class VmGuestNetworkInfoVO_ {
    public static volatile SingularAttribute<VmGuestNetworkInfoVO, Long> id;
    public static volatile SingularAttribute<VmGuestNetworkInfoVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<VmGuestNetworkInfoVO, String> vmNicUuid;
    public static volatile SingularAttribute<VmGuestNetworkInfoVO, String> ipAddress;
    public static volatile SingularAttribute<VmGuestNetworkInfoVO, String> gateway;
    public static volatile SingularAttribute<VmGuestNetworkInfoVO, String> dnsList;
    public static volatile SingularAttribute<VmGuestNetworkInfoVO, String> routeList;
    public static volatile SingularAttribute<VmGuestNetworkInfoVO, String> ipv6Address;
    public static volatile SingularAttribute<VmGuestNetworkInfoVO, String> ipv6Gateway;
    public static volatile SingularAttribute<VmGuestNetworkInfoVO, String> dns6List;
    public static volatile SingularAttribute<VmGuestNetworkInfoVO, String> route6List;
    public static volatile SingularAttribute<VmGuestNetworkInfoVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmGuestNetworkInfoVO, Timestamp> lastOpDate;
}

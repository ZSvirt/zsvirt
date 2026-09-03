package org.zstack.header.host;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(HostIpmiVO.class)
public class HostIpmiVO_ {
    public static volatile SingularAttribute<HostIpmiVO, String> uuid;
    public static volatile SingularAttribute<HostIpmiVO, String> ipmiAddress;
    public static volatile SingularAttribute<HostIpmiVO, String> ipmiUsername;
    public static volatile SingularAttribute<HostIpmiVO, Integer> ipmiPort;
    public static volatile SingularAttribute<HostIpmiVO, String> ipmiPassword;
    public static volatile SingularAttribute<HostIpmiVO, HostPowerStatus> ipmiPowerStatus;
}

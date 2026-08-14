package org.zstack.header.host;

import org.zstack.header.host.HostPowerStatus;
import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HostIpmiVO.class)
public class HostIpmiVO_ {
    public static volatile SingularAttribute<HostIpmiVO, String> uuid;
    public static volatile SingularAttribute<HostIpmiVO, String> ipmiAddress;
    public static volatile SingularAttribute<HostIpmiVO, String> ipmiUsername;
    public static volatile SingularAttribute<HostIpmiVO, Integer> ipmiPort;
    public static volatile SingularAttribute<HostIpmiVO, String> ipmiPassword;
    public static volatile SingularAttribute<HostIpmiVO, HostPowerStatus> ipmiPowerStatus;
}

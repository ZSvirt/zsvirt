package org.zstack.header.vm;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmNicVO.class)
public class VmNicVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VmNicVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<VmNicVO, String> internalName;
    public static volatile SingularAttribute<VmNicVO, String> usedIpUuid;
    public static volatile SingularAttribute<VmNicVO, String> l3NetworkUuid;
    public static volatile SingularAttribute<VmNicVO, String> netmask;
    public static volatile SingularAttribute<VmNicVO, String> gateway;
    public static volatile SingularAttribute<VmNicVO, Integer> ipVersion;
    public static volatile SingularAttribute<VmNicVO, String> ip;
    public static volatile SingularAttribute<VmNicVO, String> mac;
    public static volatile SingularAttribute<VmNicVO, String> hypervisorType;
    public static volatile SingularAttribute<VmNicVO, String> metaData;
    public static volatile SingularAttribute<VmNicVO, Integer> deviceId;
    public static volatile SingularAttribute<VmNicVO, String> driverType;
    public static volatile SingularAttribute<VmNicVO, String> type;
    public static volatile SingularAttribute<VmNicVO, VmNicState> state;
    public static volatile SingularAttribute<VmNicVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmNicVO, Timestamp> lastOpDate;
}

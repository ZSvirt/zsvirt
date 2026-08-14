package org.zstack.network.hostNetworkInterface.lldp.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HostNetworkInterfaceLldpVO.class)
public class HostNetworkInterfaceLldpVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<HostNetworkInterfaceLldpVO, String> interfaceUuid;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpVO, String> mode;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpVO, Timestamp> lastOpDate;
}

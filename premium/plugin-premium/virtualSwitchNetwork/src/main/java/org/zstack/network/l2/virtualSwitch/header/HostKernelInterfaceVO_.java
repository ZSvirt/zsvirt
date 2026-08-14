package org.zstack.network.l2.virtualSwitch.header;

import java.sql.Timestamp;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import org.zstack.header.vo.ResourceVO_;

@StaticMetamodel(HostKernelInterfaceVO.class)
public class HostKernelInterfaceVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<HostKernelInterfaceVO, String> name;
    public static volatile SingularAttribute<HostKernelInterfaceVO, String> description;
    public static volatile SingularAttribute<HostKernelInterfaceVO, String> hostUuid;
    public static volatile SingularAttribute<HostKernelInterfaceVO, String> l2NetworkUuid;
    public static volatile SingularAttribute<HostKernelInterfaceVO, String> l3NetworkUuid;
    public static volatile SingularAttribute<HostKernelInterfaceVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostKernelInterfaceVO, Timestamp> lastOpDate;
}

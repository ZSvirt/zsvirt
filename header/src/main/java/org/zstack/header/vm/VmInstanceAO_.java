package org.zstack.header.vm;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmInstanceAO.class)
public class VmInstanceAO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VmInstanceAO, String> name;
    public static volatile SingularAttribute<VmInstanceAO, String> description;
    public static volatile SingularAttribute<VmInstanceAO, String> zoneUuid;
    public static volatile SingularAttribute<VmInstanceAO, String> clusterUuid;
    public static volatile SingularAttribute<VmInstanceAO, String> imageUuid;
    public static volatile SingularAttribute<VmInstanceAO, String> rootVolumeUuid;
    public static volatile SingularAttribute<VmInstanceAO, String> instanceOfferingUuid;
    public static volatile SingularAttribute<VmInstanceAO, String> defaultL3NetworkUuid;
    public static volatile SingularAttribute<VmInstanceAO, String> hostUuid;
    public static volatile SingularAttribute<VmInstanceAO, String> lastHostUuid;
    public static volatile SingularAttribute<VmInstanceAO, String> type;
    public static volatile SingularAttribute<VmInstanceAO, String> platform;
    public static volatile SingularAttribute<VmInstanceAO, String> architecture;
    public static volatile SingularAttribute<VmInstanceAO, String> guestOsType;
    public static volatile SingularAttribute<VmInstanceAO, String> hypervisorType;
    public static volatile SingularAttribute<VmInstanceAO, String> allocatorStrategy;
    public static volatile SingularAttribute<VmInstanceAO, Long> internalId;
    public static volatile SingularAttribute<VmInstanceAO, Long> memorySize;
    public static volatile SingularAttribute<VmInstanceAO, Long> reservedMemorySize;
    public static volatile SingularAttribute<VmInstanceAO, Integer> cpuNum;
    public static volatile SingularAttribute<VmInstanceAO, Long> cpuSpeed;
    public static volatile SingularAttribute<VmInstanceAO, Timestamp> createDate;
    public static volatile SingularAttribute<VmInstanceAO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<VmInstanceAO, VmInstanceState> state;
}

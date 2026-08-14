package org.zstack.header.host;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 */
@StaticMetamodel(HostAO.class)
public class HostAO_ extends ResourceVO_ {
    public static volatile SingularAttribute<HostAO, String> zoneUuid;
    public static volatile SingularAttribute<HostAO, String> name;
    public static volatile SingularAttribute<HostAO, String> clusterUuid;
    public static volatile SingularAttribute<HostAO, String> description;
    public static volatile SingularAttribute<HostAO, String> managementIp;
    public static volatile SingularAttribute<HostAO, HostState> state;
    public static volatile SingularAttribute<HostAO, HostStatus> status;
    public static volatile SingularAttribute<HostAO, String> hypervisorType;
    public static volatile SingularAttribute<HostAO, Timestamp> createDate;
    public static volatile SingularAttribute<HostAO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<HostAO, String> architecture;
    public static volatile SingularAttribute<HostAO, String> nqn;
    public static volatile SingularAttribute<HostAO, String> hostname;
}

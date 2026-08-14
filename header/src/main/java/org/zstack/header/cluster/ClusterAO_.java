package org.zstack.header.cluster;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 */
@StaticMetamodel(ClusterAO.class)
public class ClusterAO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ClusterAO, String> zoneUuid;
    public static volatile SingularAttribute<ClusterAO, String> description;
    public static volatile SingularAttribute<ClusterAO, String> name;
    public static volatile SingularAttribute<ClusterAO, ClusterState> state;
    public static volatile SingularAttribute<ClusterAO, String> hypervisorType;
    public static volatile SingularAttribute<ClusterAO, String> type;
    public static volatile SingularAttribute<ClusterAO, String> architecture;
    public static volatile SingularAttribute<ClusterAO, String> managementNodeId;
    public static volatile SingularAttribute<ClusterAO, Timestamp> createDate;
    public static volatile SingularAttribute<ClusterAO, Timestamp> lastOpDate;
}

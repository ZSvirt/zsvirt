package org.zstack.header.vpc.ha;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VpcHaGroupMonitorIpVO.class)
public class VpcHaGroupMonitorIpVO_ {
    public static volatile SingularAttribute<VpcHaGroupMonitorIpVO, Long> id;
    public static volatile SingularAttribute<VpcHaGroupMonitorIpVO, String> vpcHaRouterUuid;
    public static volatile SingularAttribute<VpcHaGroupMonitorIpVO, String> monitorIp;
    public static volatile SingularAttribute<VpcHaGroupMonitorIpVO, Timestamp> createDate;
    public static volatile SingularAttribute<VpcHaGroupMonitorIpVO, Timestamp> lastOpDate;
}

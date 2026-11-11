package org.zstack.network.zns.entity;

import java.sql.Timestamp;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
@StaticMetamodel(ZnsSegmentRefVO.class)
public class ZnsSegmentRefVO_ {
    public static volatile SingularAttribute<ZnsSegmentRefVO, String> l2PortGroupUuid;
    public static volatile SingularAttribute<ZnsSegmentRefVO, String> tenantRouterUuid;
    public static volatile SingularAttribute<ZnsSegmentRefVO, String> znsInfoUuid;
    public static volatile SingularAttribute<ZnsSegmentRefVO, String> znsSegmentUuid;
    public static volatile SingularAttribute<ZnsSegmentRefVO, String> znsTransportZoneUuid;
    public static volatile SingularAttribute<ZnsSegmentRefVO, Integer> virtualNetworkId;
    public static volatile SingularAttribute<ZnsSegmentRefVO, Long> configVersion;
    public static volatile SingularAttribute<ZnsSegmentRefVO, String> state;
    public static volatile SingularAttribute<ZnsSegmentRefVO, String> lastOperation;
    public static volatile SingularAttribute<ZnsSegmentRefVO, String> lastError;
    public static volatile SingularAttribute<ZnsSegmentRefVO, Timestamp> lastSyncDate;
}

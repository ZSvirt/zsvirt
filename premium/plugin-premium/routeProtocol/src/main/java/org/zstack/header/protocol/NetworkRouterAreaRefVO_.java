package org.zstack.header.protocol;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(NetworkRouterAreaRefVO.class)
public class NetworkRouterAreaRefVO_ {
    public static volatile SingularAttribute<NetworkRouterAreaRefVO, String> uuid;
    public static volatile SingularAttribute<NetworkRouterAreaRefVO, String> vRouterUuid;
    public static volatile SingularAttribute<NetworkRouterAreaRefVO, String> applianceVmType;
    public static volatile SingularAttribute<NetworkRouterAreaRefVO, String> routerAreaUuid;
    public static volatile SingularAttribute<NetworkRouterAreaRefVO, String> l3NetworkUuid;
    public static SingularAttribute<NetworkRouterAreaRefVO, Timestamp> createDate;
    public static SingularAttribute<NetworkRouterAreaRefVO, Timestamp> lastOpDate;
}

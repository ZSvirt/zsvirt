package org.zstack.header.network.l3;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(IpUseVO.class)
public class IpUseVO_ {
    public static volatile SingularAttribute<IpUseVO, String> uuid;
    public static volatile SingularAttribute<IpUseVO, String> serviceId;
    public static volatile SingularAttribute<IpUseVO, String> use;
    public static volatile SingularAttribute<IpUseVO, String> usedIpUuid;
    public static volatile SingularAttribute<IpUseVO, String> details;
    public static volatile SingularAttribute<IpUseVO, Timestamp> createDate;
    public static volatile SingularAttribute<IpUseVO, Timestamp> lastOpDate;
}

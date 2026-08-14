package org.zstack.header.network.l3;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(UsedIpVO.class)
public class UsedIpVO_ {
    public static volatile SingularAttribute<UsedIpVO, String> uuid;
    public static volatile SingularAttribute<UsedIpVO, String> ipRangeUuid;
    public static volatile SingularAttribute<UsedIpVO, String> l3NetworkUuid;
    public static volatile SingularAttribute<UsedIpVO, Integer> ipVersion;
    public static volatile SingularAttribute<UsedIpVO, String> ip;
    public static volatile SingularAttribute<UsedIpVO, String> netmask;
    public static volatile SingularAttribute<UsedIpVO, String> usedFor;
    public static volatile SingularAttribute<UsedIpVO, String> metaData;
    public static volatile SingularAttribute<UsedIpVO, Long> ipInLong;
    public static volatile SingularAttribute<UsedIpVO, String> vmNicUuid;
    public static volatile SingularAttribute<UsedIpVO, String> gateway;
    public static volatile SingularAttribute<UsedIpVO, Timestamp> createDate;
    public static volatile SingularAttribute<UsedIpVO, Timestamp> lastOpDate;
}

package org.zstack.header.network.l3;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(L3NetworkDnsVO.class)
public class L3NetworkDnsVO_ {
    public static volatile SingularAttribute<L3NetworkDnsVO, Long> id;
    public static volatile SingularAttribute<L3NetworkDnsVO, String> l3NetworkUuid;
    public static volatile SingularAttribute<L3NetworkDnsVO, String> dns;
    public static volatile SingularAttribute<L3NetworkDnsVO, Integer> sortKey;
    public static volatile SingularAttribute<L3NetworkDnsVO, Timestamp> createDate;
    public static volatile SingularAttribute<L3NetworkDnsVO, Timestamp> lastOpDate;
}

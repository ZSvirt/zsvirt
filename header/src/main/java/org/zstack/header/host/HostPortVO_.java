package org.zstack.header.host;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HostPortVO.class)
public class HostPortVO_ {
    public static volatile SingularAttribute<HostPortVO, Long> id;
    public static volatile SingularAttribute<HostPortVO, String> hostUuid;
    public static volatile SingularAttribute<HostPortVO, Integer> port;
    public static volatile SingularAttribute<HostPortVO, String> usage;
    public static volatile SingularAttribute<HostPortVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostPortVO, Timestamp> lastOpDate;
}

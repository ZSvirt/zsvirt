package org.zstack.header.network.l3;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ReservedIpRangeVO.class)
public class ReservedIpRangeVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ReservedIpRangeVO, String> name;
    public static volatile SingularAttribute<ReservedIpRangeVO, String> l3NetworkUuid;
    public static volatile SingularAttribute<ReservedIpRangeVO, String> description;
    public static volatile SingularAttribute<ReservedIpRangeVO, Integer> ipVersion;
    public static volatile SingularAttribute<ReservedIpRangeVO, String> startIp;
    public static volatile SingularAttribute<ReservedIpRangeVO, String> endIp;
    public static volatile SingularAttribute<ReservedIpRangeVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<ReservedIpRangeVO, Timestamp> createDate;
}

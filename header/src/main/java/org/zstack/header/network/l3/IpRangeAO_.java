package org.zstack.header.network.l3;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(IpRangeAO.class)
public class IpRangeAO_ extends ResourceVO_ {
    public static volatile SingularAttribute<IpRangeAO, String> name;
    public static volatile SingularAttribute<IpRangeAO, String> l3NetworkUuid;
    public static volatile SingularAttribute<IpRangeAO, String> description;
    public static volatile SingularAttribute<IpRangeAO, String> startIp;
    public static volatile SingularAttribute<IpRangeAO, String> endIp;
    public static volatile SingularAttribute<IpRangeAO, String> netmask;
    public static volatile SingularAttribute<IpRangeAO, String> networkCidr;
    public static volatile SingularAttribute<IpRangeAO, String> gateway;
    public static volatile SingularAttribute<IpRangeAO, Integer> ipVersion;
    public static volatile SingularAttribute<IpRangeAO, String> addressMode;
    public static volatile SingularAttribute<IpRangeAO, Integer> prefixLen;
    public static volatile SingularAttribute<IpRangeAO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<IpRangeAO, Timestamp> createDate;
}

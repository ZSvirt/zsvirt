package org.zstack.network.service.vip;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VipVO.class)
public class VipVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VipVO, String> name;
    public static volatile SingularAttribute<VipVO, String> description;
    public static volatile SingularAttribute<VipVO, String> serviceProvider;
    public static volatile SingularAttribute<VipVO, String> useFor;
    public static volatile SingularAttribute<VipVO, String> l3NetworkUuid;
    public static volatile SingularAttribute<VipVO, String> ipRangeUuid;
    public static volatile SingularAttribute<VipVO, String> ip;
    public static volatile SingularAttribute<VipVO, String> usedIpUuid;
    public static volatile SingularAttribute<VipVO, String> gateway;
    public static volatile SingularAttribute<VipVO, String> netmask;
    public static volatile SingularAttribute<VipVO, Integer> prefixLen;
    public static volatile SingularAttribute<VipVO, VipState> state;
    public static volatile SingularAttribute<VipVO, Boolean> system;
    public static volatile SingularAttribute<VipVO, Timestamp> createDate;
    public static volatile SingularAttribute<VipVO, Timestamp> lastOpDate;
}

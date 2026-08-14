package org.zstack.network.service.vip;

/**
 * @author: zhanyong.miao
 * @date: 2019-05-06
 **/

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VipNetworkServicesRefVO.class)
public class VipNetworkServicesRefVO_ {
    public static volatile SingularAttribute<VipNetworkServicesRefVO, String> uuid;
    public static volatile SingularAttribute<VipNetworkServicesRefVO, String> serviceType;
    public static volatile SingularAttribute<VipNetworkServicesRefVO, String> vipUuid;
    public static SingularAttribute<VipNetworkServicesRefVO, Timestamp> lastOpDate;
    public static SingularAttribute<VipNetworkServicesRefVO, Timestamp> createDate;
}

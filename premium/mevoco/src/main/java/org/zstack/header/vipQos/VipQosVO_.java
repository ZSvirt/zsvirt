package org.zstack.header.vipQos;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VipQosVO.class)
public class VipQosVO_ {
    public static volatile SingularAttribute<VipQosVO, String> uuid;
    public static volatile SingularAttribute<VipQosVO, String> vipUuid;
    public static volatile SingularAttribute<VipQosVO, Integer> port;
    public static volatile SingularAttribute<VipQosVO, Long> inboundBandwidth;
    public static volatile SingularAttribute<VipQosVO, Long> outboundBandwidth;
    public static volatile SingularAttribute<VipQosVO, String>  type;
    public static volatile SingularAttribute<VipQosVO, Timestamp> createDate;
    public static volatile SingularAttribute<VipQosVO, Timestamp> lastOpDate;
}

package org.zstack.network.service.vip;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by weiwang on 02/11/2017
 */
@StaticMetamodel(VipPeerL3NetworkRefVO.class)
public class VipPeerL3NetworkRefVO_ {
    public static volatile SingularAttribute<VipPeerL3NetworkRefVO, String> vipUuid;
    public static volatile SingularAttribute<VipPeerL3NetworkRefVO, String> l3NetworkUuid;
    public static volatile SingularAttribute<VipPeerL3NetworkRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<VipPeerL3NetworkRefVO, Timestamp> lastOpDate;
}

package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.zstack.header.network.l2.L2NetworkVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by weiwang on 02/03/2017.
 */
@StaticMetamodel(VxlanNetworkVO.class)
public class VxlanNetworkVO_ extends L2NetworkVO_ {
    public static volatile SingularAttribute<VxlanNetworkVO, Integer> vni;
    public static volatile SingularAttribute<VxlanNetworkVO, String> poolUuid;
}

package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.network.l2.L2NetworkVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(L2VirtualSwitchNetworkVO.class)
public class L2VirtualSwitchNetworkVO_ extends L2NetworkVO_ {
    public static volatile SingularAttribute<L2VirtualSwitchNetworkVO, Boolean> isDistributed;
    public static volatile SingularAttribute<L2VirtualSwitchNetworkVO, Integer> vSwitchIndex;
}

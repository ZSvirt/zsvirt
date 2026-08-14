package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.network.l2.L2NetworkVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(L2PortGroupNetworkVO.class)
public class L2PortGroupNetworkVO_ extends L2NetworkVO_ {
    public static volatile SingularAttribute<L2PortGroupNetworkVO, String> vSwitchUuid;
    public static volatile SingularAttribute<L2PortGroupNetworkVO, PortGroupVlanMode> vlanMode;
    public static volatile SingularAttribute<L2PortGroupNetworkVO, Integer> vlanId;
    public static volatile SingularAttribute<L2PortGroupNetworkVO, String> vlanRanges;
}

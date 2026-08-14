package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.network.l3.L3NetworkVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(PortGroupVO.class)
public class PortGroupVO_ extends L3NetworkVO_ {
    public static volatile SingularAttribute<PortGroupVO, String> vSwitchUuid;
    public static volatile SingularAttribute<PortGroupVO, PortGroupVlanMode> vlanMode;
    public static volatile SingularAttribute<PortGroupVO, Integer> vlanId;
    public static volatile SingularAttribute<PortGroupVO, String> vlanRanges;
}

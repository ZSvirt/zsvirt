package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.network.l2.L2NetworkHostRefVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(UplinkGroupVO.class)
public class UplinkGroupVO_ extends L2NetworkHostRefVO_ {
    public static volatile SingularAttribute<UplinkGroupVO, String> interfaceName;
    public static volatile SingularAttribute<UplinkGroupVO, UplinkGroupType> type;
    public static volatile SingularAttribute<UplinkGroupVO, String> bondingUuid;
    public static volatile SingularAttribute<UplinkGroupVO, String> interfaceUuid;
}

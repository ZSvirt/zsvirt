package org.zstack.network.service.vip;

import org.zstack.utils.RangeSet;
import org.zstack.utils.VipUseForList;

import java.util.List;

public interface  VipGetUsedPortRangeExtensionPoint {
    RangeSet getVipUsePortRange(String vipUuid, String protocol, VipUseForList useForList);
}

package org.zstack.network.service.eip;

import org.zstack.network.service.vip.VipInventory;
import java.util.List;

public interface GetL3NetworkForEipInVirtualRouterExtensionPoint {
    List<String> getL3NetworkForEipInVirtualRouter(String networkServiceProviderType, VipInventory vip, List<String> l3Uuids);
}

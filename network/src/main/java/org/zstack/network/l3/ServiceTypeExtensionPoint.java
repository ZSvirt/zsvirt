package org.zstack.network.l3;

import java.util.List;

public interface ServiceTypeExtensionPoint {
    void syncManagementServiceTypeExtensionPoint(List<String> hostUuids, String interfaceName, Integer virtualNetworkId, boolean isDelete);
}


package org.zstack.network.service.flat;

import java.util.List;

public interface L3NetworkGetIpStatisticExtensionPoint {
    String getType();
    List<String> getParentUuid(String uuid, String vipUuid);
    String getResourceOwnerUuid(String usedIpUuid);
    Long countUsedIp(String l3NetworkUuid, String ip);
}

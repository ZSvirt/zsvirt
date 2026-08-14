package org.zstack.header.allocator;

import org.zstack.header.network.l3.AllocateIpMsg;

public interface BeforeAllocateIpExtensionPoint {
    String allocateIpBySdn(AllocateIpMsg msg, String bmUuid, String mac, String switchInfo);
    void releaseIpFromSdn(String nicUuid, String l3NetworkUuid);
}

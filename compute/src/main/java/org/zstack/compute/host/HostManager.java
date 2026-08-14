package org.zstack.compute.host;

import org.zstack.header.host.HostBaseExtensionFactory;
import org.zstack.header.host.HypervisorFactory;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.Message;

public interface HostManager {
    HypervisorFactory getHypervisorFactory(HypervisorType type);

    void handleMessage(Message msg);

    HostBaseExtensionFactory getHostBaseExtensionFactory(Message msg);
}

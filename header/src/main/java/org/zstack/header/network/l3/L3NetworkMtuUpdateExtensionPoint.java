package org.zstack.header.network.l3;

import org.zstack.header.core.Completion;

public interface L3NetworkMtuUpdateExtensionPoint {
    boolean supports(String l3NetworkUuid);

    void updateMtu(String l3NetworkUuid, int mtu, Completion completion);
}

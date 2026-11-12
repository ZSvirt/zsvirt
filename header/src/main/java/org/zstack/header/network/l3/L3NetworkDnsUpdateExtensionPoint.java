package org.zstack.header.network.l3;

import org.zstack.header.core.Completion;

public interface L3NetworkDnsUpdateExtensionPoint {
    void afterDnsUpdated(String l3NetworkUuid, Integer ipVersion);

    default boolean supportsDnsUpdate(String l3NetworkUuid) {
        return false;
    }

    default void updateDns(String l3NetworkUuid, Integer ipVersion, Completion completion) {
        afterDnsUpdated(l3NetworkUuid, ipVersion);
        completion.success();
    }
}

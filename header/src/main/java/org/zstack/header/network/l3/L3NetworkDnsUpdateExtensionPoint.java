package org.zstack.header.network.l3;

public interface L3NetworkDnsUpdateExtensionPoint {
    void afterDnsUpdated(String l3NetworkUuid, Integer ipVersion);
}

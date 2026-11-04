package org.zstack.header.host;

/**
 * Created by frank on 9/19/2015.
 */
public interface HostAfterConnectedExtensionPoint {
    default void afterHostConnected(HostInventory host, ConnectHostInfo hostInfo) {
        afterHostConnected(host);
    }

    default void afterHostConnected(HostInventory host) {}
}

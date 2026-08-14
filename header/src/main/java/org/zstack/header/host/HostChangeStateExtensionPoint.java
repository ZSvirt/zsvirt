package org.zstack.header.host;


public interface HostChangeStateExtensionPoint {
    void preChangeHostState(HostInventory inventory, HostStateEvent event, HostState nextState) throws HostException;

    void beforeChangeHostState(HostInventory inventory, HostStateEvent event, HostState nextState);

    void afterChangeHostState(HostInventory inventory, HostStateEvent event, HostState previousState);
}

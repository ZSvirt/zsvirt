package org.zstack.compute.bonding;

public interface HostNetworkBondingManager {
    HostNetworkBondingFactory getHostNetworkBondingFactory(String type);
}

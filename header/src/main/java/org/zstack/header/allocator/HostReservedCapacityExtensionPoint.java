package org.zstack.header.allocator;

/**
 */
public interface HostReservedCapacityExtensionPoint {
    String getHypervisorTypeForHostReserveCapacityExtension();

    ReservedHostCapacity getReservedHostCapacity(String hostUuid);
}

package org.zstack.compute.allocator;

import org.zstack.header.allocator.ReservedHostCapacity;
import org.zstack.header.host.HostVO;

import java.util.List;

/**
 */
public interface HostCapacityReserveManager {
    List<HostVO> filterOutHostsByReservedCapacity(List<HostVO> candidates, long requiredCpu, long requiredMemory);

    ReservedHostCapacity getReservedHostCapacityByZones(List<String> zoneUuids, String hypervisorType);

    ReservedHostCapacity getReservedHostCapacityByClusters(List<String> clusterUuids, String hypervisorType);

    ReservedHostCapacity getReservedHostCapacityByHosts(List<String> hostUuids);

    void reserveCapacity(String hostUuid, long requiredCpu, long requiredMemory, boolean skipCheck);
}

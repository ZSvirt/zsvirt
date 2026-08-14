package org.zstack.simulator;

import org.zstack.header.allocator.HostReservedCapacityExtensionPoint;
import org.zstack.header.allocator.ReservedHostCapacity;
import org.zstack.header.simulator.SimulatorConstant;
import org.zstack.utils.SizeUtils;

/**
 */
public class SimulatorHostReservedCapacityExtension implements HostReservedCapacityExtensionPoint {
    public volatile String reservedCpu = "0b";
    public volatile String reservedMemory = "0b";

    @Override
    public String getHypervisorTypeForHostReserveCapacityExtension() {
        return SimulatorConstant.SIMULATOR_HYPERVISOR_TYPE;
    }

    @Override
    public ReservedHostCapacity getReservedHostCapacity(String hostUuid) {
        ReservedHostCapacity c = new ReservedHostCapacity();
        c.setReservedMemoryCapacity(SizeUtils.sizeStringToBytes(reservedMemory));
        c.setReservedCpuCapacity(SizeUtils.sizeStringToBytes(reservedCpu));
        return c;
    }
}

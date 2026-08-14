package org.zstack.simulator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.cluster.ReportHostCapacityMessage;
import org.zstack.header.simulator.SimulatorConnectExtensionPoint;
import org.zstack.header.simulator.SimulatorConnection;

public class SimulatorConnectExtensionPointHostCapacityBackend implements SimulatorConnectExtensionPoint {
    @Autowired
    private CloudBus bus;

    @Override
    public String connect(SimulatorConnection connection) {
        ReportHostCapacityMessage msg = new ReportHostCapacityMessage();
        msg.setHostUuid(connection.getHostUuid());
        msg.setCpuNum((int) connection.getTotalCpu());
        msg.setTotalMemory(connection.getTotalMemory());
        msg.setUsedCpu(connection.getUsedCpu());
        msg.setUsedMemory(connection.getUsedMemory());
        msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        bus.send(msg);
        return null;
    }

}

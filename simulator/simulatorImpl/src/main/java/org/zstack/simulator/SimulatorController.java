package org.zstack.simulator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostConstant;
import org.zstack.header.simulator.ChangeVmStateOnSimulatorHostMsg;
import org.zstack.header.simulator.RemoveVmOnSimulatorMsg;
import org.zstack.header.simulator.SimulatorHostConnectionControlMsg;
import org.zstack.header.vm.VmInstanceState;

public class SimulatorController {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    
    public void setSimulatorHostConnectionState(String uuid, boolean isDisconnected) {
        SimulatorHostConnectionControlMsg msg = new SimulatorHostConnectionControlMsg();
        msg.setHostUuid(uuid);
        msg.setDisconnected(isDisconnected);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, uuid);
        bus.call(msg);
    }
    
    public void setVmStateOnSimulatorHost(String hostUuid, String vmUuid, VmInstanceState vmState) {
        ChangeVmStateOnSimulatorHostMsg msg = new ChangeVmStateOnSimulatorHostMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmUuid(vmUuid);
        msg.setVmState(vmState.toString());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.call(msg);
    }
    
    public void removeVmOnSimulatorHost(String hostUuid, String vmUuid) {
        RemoveVmOnSimulatorMsg msg = new RemoveVmOnSimulatorMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmUuid(vmUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.call(msg);
    }
}

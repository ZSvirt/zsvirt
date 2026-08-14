package org.zstack.simulator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmTracer;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostPingTaskExtensionPoint;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.simulator.SimulatorConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;

public class SimulatorVmSyncPingTask extends VmTracer implements HostPingTaskExtensionPoint {
    private static final CLogger logger = Utils.getLogger(SimulatorVmSyncPingTask.class);

    @Autowired
    private SimulatorConfig config;
    
    @Override
    public void executeTaskAlongWithPingTask(HostInventory inv) {
        logger.debug(String.format("SimulatorHost[uuid:%s] is tracing vm status", inv.getUuid()));
        Map<String, VmInstanceState> vms = new HashMap<String, VmInstanceState>();

        Map<String, VmInstanceState> curr = config.getVmOnHost(inv.getUuid());
        if (curr == null) {
            super.reportVmState(inv.getUuid(), vms, null, null);
            return;
        }

        for (Map.Entry<String, VmInstanceState> e : curr.entrySet()) {
            if (e.getValue() == VmInstanceState.Running || e.getValue() == VmInstanceState.Unknown) {
                vms.put(e.getKey(), e.getValue());
            }
        }
        
        super.reportVmState(inv.getUuid(), vms, null, null);
    }

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.valueOf(SimulatorConstant.SIMULATOR_HYPERVISOR_TYPE);
    }
}

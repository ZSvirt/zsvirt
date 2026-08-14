package org.zstack.network.service.virtualrouter.lifecycle;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.network.service.virtualrouter.VirtualRouterPingTracker;

import java.util.Map;

/**
 * Created by frank on 6/29/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UntrackVirtualRouterVmFlow extends NoRollbackFlow {
    @Autowired
    private VirtualRouterPingTracker tracker;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        tracker.untrack(spec.getVmInventory().getUuid());
        trigger.next();
    }
}

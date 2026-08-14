package org.zstack.xdragon;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.storage.primary.sharedblock.SharedBlockImageStoreKvmFactory;

import java.util.Map;

public class SharedBlockImageStoreXDragonFactory extends SharedBlockImageStoreKvmFactory {
    @Override
    public String getHypervisorType() {
        return XDragonConstant.HYPERVISOR_TYPE;
    }

    @Override
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        if (XDragonConstant.HYPERVISOR_TYPE.equals(context.getInventory().getHypervisorType())) {
            return super.createKvmHostConnectingFlow(context);
        }

        return new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                trigger.next();
            }
        };
    }
}

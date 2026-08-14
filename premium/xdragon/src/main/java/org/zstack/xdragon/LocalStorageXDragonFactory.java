package org.zstack.xdragon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.storage.primary.local.LocalStorageHypervisorBackend;
import org.zstack.storage.primary.local.LocalStorageHypervisorFactory;
import org.zstack.storage.primary.local.LocalStorageKvmFactory;

import java.util.Map;

public class LocalStorageXDragonFactory implements LocalStorageHypervisorFactory, KVMHostConnectExtensionPoint {
    @Autowired
    @Qualifier("LocalStorageKvmFactory")
    private LocalStorageKvmFactory factory;

    @Override
    public String getHypervisorType() {
        return XDragonConstant.HYPERVISOR_TYPE;
    }

    @Override
    public LocalStorageHypervisorBackend getHypervisorBackend(PrimaryStorageVO vo) {
        return factory.getHypervisorBackend(vo);
    }

    @Override
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        if (context.getInventory() != null && XDragonConstant.HYPERVISOR_TYPE.equals(context.getInventory().getHypervisorType())) {
            return factory.createKvmHostConnectingFlow(context);
        }

        return new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                trigger.next();
            }
        };
    }
}

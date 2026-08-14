package org.zstack.xdragon;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HypervisorType;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMSecurityGroupBackend;

import java.util.Map;

public class XDragonSecurityGroupBackend extends KVMSecurityGroupBackend {
    @Override
    public HypervisorType getSecurityGroupBackendHypervisorType() {
        return HypervisorType.valueOf(XDragonConstant.HYPERVISOR_TYPE);
    }

    @Override
    public void afterHostConnected(HostInventory host) {
        if (XDragonConstant.HYPERVISOR_TYPE.equals(host.getHypervisorType())) {
            super.afterHostConnected(host);
        }
    }
}

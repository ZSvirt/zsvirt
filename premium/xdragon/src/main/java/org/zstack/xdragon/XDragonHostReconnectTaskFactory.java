package org.zstack.xdragon;

import org.zstack.kvm.KVMHostReconnectTaskFactory;

public class XDragonHostReconnectTaskFactory extends KVMHostReconnectTaskFactory {
    @Override
    public String getHypervisorType() {
        return XDragonConstant.HYPERVISOR_TYPE;
    }
}

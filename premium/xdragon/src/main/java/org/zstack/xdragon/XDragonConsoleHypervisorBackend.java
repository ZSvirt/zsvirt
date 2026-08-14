package org.zstack.xdragon;

import org.zstack.header.host.HypervisorType;
import org.zstack.kvm.KVMConsoleHypervisorBackend;

public class XDragonConsoleHypervisorBackend extends KVMConsoleHypervisorBackend {
    @Override
    public HypervisorType getConsoleBackendHypervisorType() {
        return HypervisorType.valueOf(XDragonConstant.HYPERVISOR_TYPE);
    }
}

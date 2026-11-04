package org.zstack.xdragon;

import org.zstack.header.host.ConnectHostInfo;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HypervisorType;
import org.zstack.kvm.KVMSecurityGroupBackend;

public class XDragonSecurityGroupBackend extends KVMSecurityGroupBackend {
    @Override
    public HypervisorType getSecurityGroupBackendHypervisorType() {
        return HypervisorType.valueOf(XDragonConstant.HYPERVISOR_TYPE);
    }

    @Override
    public void afterHostConnected(HostInventory host, ConnectHostInfo hostInfo) {
        if (XDragonConstant.HYPERVISOR_TYPE.equals(host.getHypervisorType())) {
            super.afterHostConnected(host, hostInfo);
        }
    }
}

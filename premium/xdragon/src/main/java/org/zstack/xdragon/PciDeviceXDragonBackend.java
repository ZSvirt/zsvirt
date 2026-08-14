package org.zstack.xdragon;

import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HypervisorType;
import org.zstack.pciDevice.KvmPciDeviceBackend.PciDeviceKvmBackend;

public class PciDeviceXDragonBackend extends PciDeviceKvmBackend {
    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.valueOf(XDragonConstant.HYPERVISOR_TYPE);
    }

    @Override
    public void connectionReestablished(HostInventory inv) throws HostException {
        if (XDragonConstant.HYPERVISOR_TYPE.equals(inv.getHypervisorType())) {
            super.connectionReestablished(inv);
        }
    }
}

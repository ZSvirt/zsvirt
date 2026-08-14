package org.zstack.xdragon;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HypervisorType;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.usbDevice.KvmUsbDeviceBackend.UsbDeviceKvmBackend;

import java.util.Map;

public class UsbDeviceXDragonBackend extends UsbDeviceKvmBackend {
    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.valueOf(XDragonConstant.HYPERVISOR_TYPE);
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

    @Override
    public void connectionReestablished(HostInventory inv) throws HostException {
        if (XDragonConstant.HYPERVISOR_TYPE.equals(inv.getHypervisorType())) {
            super.connectionReestablished(inv);
        }
    }
}

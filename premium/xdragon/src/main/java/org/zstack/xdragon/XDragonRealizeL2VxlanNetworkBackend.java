package org.zstack.xdragon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkRealizationExtensionPoint;
import org.zstack.header.network.l2.L2NetworkType;

public class XDragonRealizeL2VxlanNetworkBackend implements L2NetworkRealizationExtensionPoint {
    @Autowired
    @Qualifier("KVMRealizeL2VxlanNetworkBackend")
    private L2NetworkRealizationExtensionPoint kvmL2extp;

    @Override
    public void realize(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        kvmL2extp.realize(l2Network, hostUuid, completion);
    }

    @Override
    public void check(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        kvmL2extp.check(l2Network, hostUuid, completion);
    }

    @Override
    public L2NetworkType getSupportedL2NetworkType() {
        return kvmL2extp.getSupportedL2NetworkType();
    }

    @Override
    public HypervisorType getSupportedHypervisorType() {
        return HypervisorType.valueOf(XDragonConstant.HYPERVISOR_TYPE);
    }

    public void delete(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        completion.success();
    }
}

package org.zstack.compute.host;

import org.zstack.header.host.HypervisorType;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.kvm.KVMConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by GuoYi on 5/3/20.
 */
public class MevocoKVMAttachL2NoVlanNetworkBackend extends MevocoKVMAttachL2NetworkBackend {
    private static final CLogger logger = Utils.getLogger(MevocoKVMAttachL2NoVlanNetworkBackend.class);

    @Override
    public L2NetworkType getSupportedL2NetworkType() {
        return L2NetworkType.valueOf(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE);
    }

    @Override
    public HypervisorType getSupportedHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }
}

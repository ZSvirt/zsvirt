package org.zstack.macvlan;

import org.zstack.header.core.Completion;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.network.l2.*;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.tag.SystemTagCreator;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class KVMRealizeL2NoVlanMacVlanBackend implements L2NetworkRealizationExtensionPoint {
    private static final L2ProviderType l2ProviderType = new L2ProviderType(KVMConstant.L2_PROVIDER_TYPE_MACVLAN);

    @Override
    public void realize(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        realize(l2Network, hostUuid, false, completion);
    }

    @Override
    public void realize(L2NetworkInventory l2Network, String hostUuid, boolean noStatusCheck, Completion completion) {
        SystemTagCreator creator = KVMSystemTags.L2_BRIDGE_NAME.newSystemTagCreator(l2Network.getUuid());
        creator.inherent = true;
        creator.ignoreIfExisting = true;
        creator.setTagByTokens(map(e(KVMSystemTags.L2_BRIDGE_NAME_TOKEN, l2Network.getPhysicalInterface())));
        creator.create();
        completion.success();
    }

    @Override
    public void check(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        completion.success();
    }

    @Override
    public L2NetworkType getSupportedL2NetworkType() {
        return L2NetworkType.valueOf(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE);
    }

    @Override
    public HypervisorType getSupportedHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public L2ProviderType getL2ProviderType() {
        return l2ProviderType;
    }

    @Override
    public void delete(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        completion.success();
    }
}

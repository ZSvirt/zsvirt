package org.zstack.network.l2.virtualSwitch;

import org.zstack.core.db.Q;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.network.l2.virtualSwitch.header.L2PortGroupNetworkVO;
import org.zstack.network.l2.virtualSwitch.header.L2PortGroupNetworkVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMCompleteNicInformationExtensionPoint;
import org.zstack.network.l2.virtualSwitch.header.VirtualSwitchConstant;
import org.zstack.network.service.MtuGetter;

public class KVMRealizeL2PortGroupLinuxBridgeBackend extends KVMRealizeL2NoVlanLinuxBridgeBackend implements
        KVMCompleteNicInformationExtensionPoint {
    @Autowired
    private PluginRegistry pluginRgty;

    public static PortGroupBackend findBackend(PluginRegistry pluginRgty, String vSwitchUuid) {
        for (PortGroupBackend backend : pluginRgty.getExtensionList(PortGroupBackend.class)) {
            if (backend.supports(vSwitchUuid)) {
                return backend;
            }
        }
        return null;
    }

    private PortGroupBackend getBackend(L2NetworkInventory l2) {
        String vSwitchUuid = Q.New(L2PortGroupNetworkVO.class).select(L2PortGroupNetworkVO_.vSwitchUuid)
                .eq(L2PortGroupNetworkVO_.uuid, l2.getUuid()).findValue();
        return findBackend(pluginRgty, vSwitchUuid);
    }

    @Override
    protected String getInterfaceName(L2NetworkInventory l2Network, String hostUuid) {
        return VirtualSwitchUtils.getInterfaceNameOfL2PortGroupOnHost(l2Network, hostUuid);
    }

    @Override
    public L2NetworkType getSupportedL2NetworkType() {
        return L2NetworkType.valueOf(VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE);
    }

    @Override
    public void check(final L2NetworkInventory l2Network, final String hostUuid, boolean noStatusCheck, final Completion completion) {
        /* virtual switch need check, port group don't need check */
        completion.success();
    }

    @Override
    public KVMAgentCommands.NicTO completeNicInformation(L2NetworkInventory l2Network, L3NetworkInventory l3Network, VmNicInventory nic) {
        PortGroupBackend backend = getBackend(l2Network);
        if (backend != null) {
            KVMAgentCommands.NicTO to = backend.completeNicInformation(l2Network, l3Network, nic);
            if (to != null) {
                return to;
            }
        }
        KVMAgentCommands.NicTO to = KVMAgentCommands.NicTO.fromVmNicInventory(nic);
        String hostUuid = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.hostUuid)
                .eq(VmInstanceVO_.uuid, nic.getVmInstanceUuid())
                .findValue();
        // if hostUuid is null, the method tries the method of the parent class
        to.setBridgeName(makeBridgeName(l2Network.getUuid(), l2Network.getVirtualNetworkId(), hostUuid));
        to.setPhysicalInterface(getInterfaceName(l2Network, hostUuid));

        to.setMtu(new MtuGetter().getMtu(l3Network.getUuid()));
        if (l2Network.getVirtualNetworkId() != 0) {
            to.setVlanId(String.valueOf(l2Network.getVirtualNetworkId()));
        }

        return to;
    }

    @Override
    public String getBridgeName(L2NetworkInventory l2Network) {
        PortGroupBackend backend = getBackend(l2Network);
        if (backend != null && backend.getBridgeName() != null) {
            return backend.getBridgeName();
        }
        return makeBridgeName(l2Network.getUuid(), l2Network.getVirtualNetworkId(), null);
    }

    @Override
    public L2NetworkType getL2NetworkTypeVmNicOn() {
        return L2NetworkType.valueOf(VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE);
    }
}

package org.zstack.appliancevm;

import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceType;
import org.zstack.kvm.KVMAddons;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStartVmAddonExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

/**
 */
public class ApplianceVmKvmBackend implements KVMStartVmAddonExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ApplianceVmKvmBackend.class);

    @Override
    public VmInstanceType getVmTypeForAddonExtension() {
        return ApplianceVmFactory.type;
    }

    @Override
    public void addAddon(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (!spec.getVmInventory().getType().equals(ApplianceVmConstant.APPLIANCE_VM_TYPE)) {
            return;
        }

        KVMAddons.Channel chan = new KVMAddons.Channel();
        cmd.setEmulateHyperV(false);
        chan.setSocketPath(makeChannelSocketPath(spec.getVmInventory().getUuid()));
        chan.setTargetName("applianceVm.vport");
        cmd.getAddons().put(KVMAddons.Channel.NAME, chan);
        logger.debug(String.format("make kvm channel device[path:%s, target:%s]", chan.getSocketPath(), chan.getTargetName()));
    }

    public String makeChannelSocketPath(String apvmuuid) {
        return PathUtil.join(ApplianceVmConstant.KVM_CHANNEL_AGENT_PATH, String.format("applianceVm.%s", apvmuuid));
    }
}

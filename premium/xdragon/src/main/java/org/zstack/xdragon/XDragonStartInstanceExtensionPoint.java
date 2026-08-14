package org.zstack.xdragon;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.PreVmInstantiateResourceExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XDragonStartInstanceExtensionPoint implements KVMStartVmExtensionPoint,
        PreVmInstantiateResourceExtensionPoint {
    private void setAddonsIfNotNull(final Map<String, Object> addons,
                                    final String key,
                                    final Object val)
    {
        if (val != null) {
            addons.put(key, val);
        }
    }

    private void changeDeviceType(VolumeTO volumeTO) {
        if (VolumeTO.FILE.equals(volumeTO.getDeviceType())) {
            volumeTO.setDeviceType("spool");
        }
    }

    private static List<String> getMacL3Mappings(final List<VmNicInventory> nics) {
        List<String> mappings = new ArrayList<>();
        for (VmNicInventory inv: nics) {
            mappings.add(String.format("%s-%s", inv.getMac(), inv.getL3NetworkUuid()));
        }

        return mappings;
    }

    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (!XDragonConstant.HYPERVISOR_TYPE.equals(host.getHypervisorType())) {
            return;
        }

        Map<String, Object> addons = cmd.getAddons();
        setAddonsIfNotNull(addons, "qemuPath", XDragonGlobalProperty.QEMU_Path);
        setAddonsIfNotNull(addons, "onCrash", XDragonGlobalProperty.ON_CRASH);
        setAddonsIfNotNull(addons, "useMemBalloon", XDragonGlobalProperty.USE_MEM_BALLOON);
        setAddonsIfNotNull(addons, "useDataPlane", XDragonGlobalProperty.USE_DATA_PLANE);
        setAddonsIfNotNull(addons, "loaderRom", XDragonGlobalProperty.LOADER_ROM);
        setAddonsIfNotNull(addons, "qemuCommandLine", XDragonGlobalProperty.QEMU_ARGS);
        setAddonsIfNotNull(addons, "noConsole", XDragonGlobalProperty.DISABLE_CONSOLE);
        setAddonsIfNotNull(addons, "vhostSrcPath", XDragonGlobalProperty.VHOST_SRC_PATH);

        final String brMode =  XDragonTapHelper.getBridgeMode(host.getClusterUuid());
        setAddonsIfNotNull(addons, "brMode", brMode);

        if (XDragonTapHelper.bridgeTypeMocbr.equals(brMode)) {
            setAddonsIfNotNull(addons, "l3mapping", getMacL3Mappings(spec.getDestNics()));
        }

        // Do not setup additional QMP channel
        if (XDragonGlobalProperty.DISABLE_QGA_CHANNEL) {
            addons.remove(KVMAddons.Channel.NAME);
        }

        // Treat it as appliance VM so to:
        //  - remove CD-ROM devices
        //  - no USB redirect
        cmd.setApplianceVm(XDragonGlobalProperty.AS_APPLIANCE_VM);

        // disable NUMA
        cmd.setUseNuma(false);

        if (!XDragonGlobalProperty.USE_DATA_PLANE) {
            changeDeviceType(cmd.getRootVolume());
            if (cmd.getDataVolumes() != null) {
                cmd.getDataVolumes().forEach(this::changeDeviceType);
            }
        }
    }

    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {
    }

    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {
    }

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec)  {
    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        if (!XDragonConstant.HYPERVISOR_TYPE.equals(spec.getDestHost().getHypervisorType())) {
            completion.success();
            return;
        }

        // We use this extension point to add the tap device to the L2 physical interface
        // used by XDragon instance.  Since there can be only one instance runnning at the
        // same time, the tap device can be safely assigned to the L2 being used.
        String br = XDragonTapHelper.getBridgeNameByL3(spec.getVmInventory().getDefaultL3NetworkUuid());
        if (br == null) {
            completion.success();
            return;
        }

        String brMode = XDragonTapHelper.getBridgeMode(spec.getDestHost().getClusterUuid());
        if (XDragonTapHelper.bridgeTypeMocbr.equals(brMode)) {
            completion.success();
            return;
        }

        new XDragonTapHelper().addtap(spec.getDestHost().getUuid(), br, completion);
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }

}

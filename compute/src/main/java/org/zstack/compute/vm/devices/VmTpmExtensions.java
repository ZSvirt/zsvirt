package org.zstack.compute.vm.devices;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.BuildVmSpecExtensionPoint;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.tpm.entity.TpmSpec;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.header.vm.VmInstanceCreateExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmMachineType;
import org.zstack.header.vm.devices.NvRamSpec;
import org.zstack.header.vm.devices.VmDevicesSpec;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.compute.vm.VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT;

public class VmTpmExtensions implements VmInstanceCreateExtensionPoint,
        BuildVmSpecExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmTpmExtensions.class);

    @Autowired
    private VmTpmManager vmTpmManager;
    @Autowired
    private ResourceConfigFacade resourceConfigFacade;
    @Autowired
    private TpmEncryptedResourceKeyBackend resourceKeyBackend;

    @Override
    public void preCreateVmInstance(CreateVmInstanceMsg msg) {
        // do-nothing
    }

    @Override
    public void afterPersistVmInstanceVO(VmInstanceVO vo, CreateVmInstanceMsg msg) {
        final VmDevicesSpec spec = msg.getDevicesSpec();
        if (spec == null || spec.getTpm() == null || !spec.getTpm().isEnable()) {
            return;
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                final TpmVO tpm = vmTpmManager.persistTpmVO(null, vo.getUuid());
                final String keyProviderUuid = spec.getTpm().getKeyProviderUuid();
                if (keyProviderUuid != null) {
                    resourceKeyBackend.attachKeyProviderToTpm(tpm.getUuid(), keyProviderUuid);
                }
            }
        }.execute();
    }

    @Override
    public void afterRollbackPersistVmInstanceVO(VmInstanceVO vo, CreateVmInstanceMsg msg) {
        String tpmUuid = Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, vo.getUuid())
                .select(TpmVO_.uuid)
                .findValue();
        if (tpmUuid == null) {
            return;
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                try {
                    resourceKeyBackend.detachKeyProviderFromTpm(tpmUuid);
                } finally {
                    vmTpmManager.deleteTpmVO(tpmUuid);
                }
            }
        }.execute();
    }

    @Override
    public void afterBuildVmSpec(VmInstanceSpec spec) {
        String vmUuid = spec.getVmInventory().getUuid();

        String tpmUuid = Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, vmUuid)
                .select(TpmVO_.uuid)
                .findValue();
        boolean needRegisterNvRam = tpmUuid != null;
        if (!needRegisterNvRam) {
            String bootMode = VmSystemTags.BOOT_MODE.getTokenByResourceUuid(vmUuid, VmSystemTags.BOOT_MODE_TOKEN);
            if (vmTpmManager.isUefiBootMode(bootMode)) {
                ResourceConfig resourceConfig = resourceConfigFacade.getResourceConfig(ENABLE_UEFI_SECURE_BOOT.getIdentity());
                needRegisterNvRam = resourceConfig.getResourceConfigValue(vmUuid, Boolean.class) == Boolean.TRUE;
            }
        }

        VmDevicesSpec devicesSpec = spec.getDevicesSpec();
        if (devicesSpec == null) {
            devicesSpec = new VmDevicesSpec();
            spec.setDevicesSpec(devicesSpec);
        }

        if (needRegisterNvRam) {
            NvRamSpec nvRamSpec = devicesSpec.getNvRam();
            if (nvRamSpec == null) {
                nvRamSpec = new NvRamSpec();
                devicesSpec.setNvRam(nvRamSpec);
            }

            nvRamSpec.setNeedRegister(true);
        }

        if (tpmUuid != null) {
            TpmSpec tpmSpec = devicesSpec.getTpm();
            if (tpmSpec == null) {
                tpmSpec = new TpmSpec();
                devicesSpec.setTpm(tpmSpec);
            }

            tpmSpec.setEnable(true);
            tpmSpec.setTpmUuid(tpmUuid);
        }

        if (needRegisterNvRam && spec.getOsSpec().getMachineType() == null) {
            spec.getOsSpec().setMachineType(VmMachineType.q35.toString());
            logger.debug(String.format(
                    "auto-set machineType to q35 for VM[uuid:%s] because NvRam/TPM registration is needed", vmUuid));
        }
    }
}

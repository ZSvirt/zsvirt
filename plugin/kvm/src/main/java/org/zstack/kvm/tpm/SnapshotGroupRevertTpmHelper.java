package org.zstack.kvm.tpm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.devices.TpmEncryptedResourceKeyBackend;
import org.zstack.core.db.Q;
import org.zstack.header.tpm.entity.TpmSpec;
import org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotGroupMsg;
import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.devices.NvRamSpec;
import org.zstack.header.vm.devices.VmDevicesSpec;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.compute.vm.VmGlobalConfig.ALLOWED_TPM_VM_WITHOUT_KMS;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SnapshotGroupRevertTpmHelper {
    private static final CLogger logger = Utils.getLogger(SnapshotGroupRevertTpmHelper.class);

    @Autowired
    private TpmEncryptedResourceKeyBackend tpmKeyBackend;

    public void setupFromApi(APICreateVmInstanceFromVolumeSnapshotGroupMsg apiMsg, CreateVmInstanceMsg cmsg) {
        String snapshotGroupUuid = apiMsg.getVolumeSnapshotGroupUuid();

        boolean resetTpm = apiMsg.getResetTpm() == Boolean.TRUE;
        List<VmHostBackupFileVO> backupFiles = Q.New(VmHostBackupFileVO.class)
                .eq(VmHostBackupFileVO_.resourceUuid, snapshotGroupUuid)
                .list();

        if (backupFiles.isEmpty()) {
            logger.debug(String.format(
                    "no VmHostBackupFileVO found for volume snapshot group[uuid:%s], skip restoring TPM/NvRam",
                    snapshotGroupUuid));
            return;
        }

        VmHostBackupFileVO tpmBackupFile = null;
        VmHostBackupFileVO nvRamBackupFile = null;
        for (VmHostBackupFileVO f : backupFiles) {
            if (f.getType() == VmHostFileType.TpmState) {
                tpmBackupFile = f;
            } else if (f.getType() == VmHostFileType.NvRam) {
                nvRamBackupFile = f;
            }
        }

        if (tpmBackupFile == null && nvRamBackupFile == null) {
            logger.debug(String.format("no TpmState or NvRam backup file found for volume snapshot group[uuid:%s]",
                    snapshotGroupUuid));
            return;
        }

        VmDevicesSpec devicesSpec = cmsg.getDevicesSpec();
        if (devicesSpec == null) {
            devicesSpec = new VmDevicesSpec();
            cmsg.setDevicesSpec(devicesSpec);
        }

        if (tpmBackupFile != null) {
            TpmSpec tpmSpec = devicesSpec.getTpm();
            if (tpmSpec == null) {
                tpmSpec = new TpmSpec();
                devicesSpec.setTpm(tpmSpec);
            }
            setupTpmSpec(snapshotGroupUuid, tpmBackupFile, tpmSpec, cmsg, resetTpm);
        }

        if (nvRamBackupFile != null) {
            NvRamSpec nvRamSpec = devicesSpec.getNvRam();
            if (nvRamSpec == null) {
                nvRamSpec = new NvRamSpec();
                devicesSpec.setNvRam(nvRamSpec);
            }
            nvRamSpec.setBackupFileUuid(nvRamBackupFile.getUuid());
            logger.debug(String.format("set NvRam restore info for volume snapshot group[uuid:%s], nvRamBackupFileUuid:%s",
                    snapshotGroupUuid, nvRamBackupFile.getUuid()));
        }
    }

    public void setupFromBackupResource(String snapshotGroupUuid, Boolean resetTpmRequested,
                                        CreateVmInstanceMsg cmsg) {
        if (resetTpmRequested == Boolean.TRUE) {
            VmDevicesSpec devicesSpec = cmsg.getDevicesSpec();
            if (devicesSpec == null) {
                devicesSpec = new VmDevicesSpec();
                cmsg.setDevicesSpec(devicesSpec);
            }
            TpmSpec tpmSpec = devicesSpec.getTpm();
            if (tpmSpec == null) {
                tpmSpec = new TpmSpec();
                devicesSpec.setTpm(tpmSpec);
            }
            tpmSpec.setEnable(true);
            tpmSpec.setKeyProviderUuid(tpmKeyBackend.defaultKeyProviderUuid());
        }

        APICreateVmInstanceFromVolumeSnapshotGroupMsg apiMsg =
                new APICreateVmInstanceFromVolumeSnapshotGroupMsg();
        apiMsg.setVolumeSnapshotGroupUuid(snapshotGroupUuid);
        apiMsg.setResetTpm(resetTpmRequested);
        setupFromApi(apiMsg, cmsg);
    }

    private void setupTpmSpec(String snapshotGroupUuid, VmHostBackupFileVO tpmBackupFile, TpmSpec tpmSpec,
                              CreateVmInstanceMsg cmsg, boolean resetTpm) {
        tpmSpec.setEnable(true);

        if (resetTpm) {
            // resetTpm=true: reset generate a new one during VM creation
            logger.debug(String.format("resetTpm is true for volume snapshot group[uuid:%s], " +
                    "will reset tpmBackupFileUuid:%s", snapshotGroupUuid, tpmBackupFile.getUuid()));
        } else {
            tpmSpec.setBackupFileUuid(tpmBackupFile.getUuid());
        }

        if (ALLOWED_TPM_VM_WITHOUT_KMS.value(Boolean.class) == Boolean.TRUE) {
            return;
        }

        if (resetTpm) {
            String defaultProviderUuid = tpmKeyBackend.defaultKeyProviderUuid();
            tpmSpec.setKeyProviderUuid(defaultProviderUuid);
            logger.info(String.format(
                    "snapshot-reset TPM target provider selected, source[snapshotGroupUuid:%s,tpmBackupFileUuid:%s], " +
                            "destination[vmResourceUuid:%s,vmName:%s,providerUuid:%s]",
                    snapshotGroupUuid, tpmBackupFile.getUuid(),
                    cmsg.getResourceUuid(), cmsg.getName(), defaultProviderUuid));
            return;
        }

        String keyProviderName = KVMSystemTags.TPM_KEY_PROVIDER_NAME
                .getTokenByResourceUuid(tpmBackupFile.getUuid(), KVMSystemTags.TPM_KEY_PROVIDER_NAME_TOKEN);
        if (keyProviderName == null) {
            logger.warn(String.format(
                    "failed to find keyProvider from snapshotGroup[uuid:%s] by tpmBackupFile[uuid:%s]",
                    snapshotGroupUuid, tpmBackupFile.getUuid()));
            if (tpmSpec.getKeyProviderUuid() == null) {
                tpmSpec.setKeyProviderUuid(tpmKeyBackend.defaultKeyProviderUuid());
            }
            return;
        }

        String keyProviderUuid = tpmKeyBackend.findKeyProviderUuidByName(keyProviderName);
        if (keyProviderUuid == null) {
            logger.warn(String.format(
                    "failed to resolve keyProvider[name:%s] from snapshotGroup[uuid:%s] by tpmBackupFile[uuid:%s], keep keyProviderUuid unset",
                    keyProviderName, snapshotGroupUuid, tpmBackupFile.getUuid()));
            return;
        }

        tpmSpec.setKeyProviderUuid(keyProviderUuid);
    }
}

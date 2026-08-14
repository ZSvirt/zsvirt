package org.zstack.softwarePackage.compute;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.backup.BackupStorageState;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.kvm.KVMConstant;
import org.zstack.softwarePackage.SoftwarePackageConstant;
import org.zstack.softwarePackage.compute.client.ShellCommandUtils;
import org.zstack.softwarePackage.compute.client.SoftwarePackageBackupStorageUtils;
import org.zstack.softwarePackage.entity.SoftwarePackageStatus;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;
import org.zstack.softwarePackage.entity.SoftwarePackageVO_;
import org.zstack.softwarePackage.entity.UpgradeType;
import org.zstack.softwarePackage.header.*;

import javax.persistence.Tuple;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static org.zstack.core.Platform.*;
import static org.zstack.softwarePackage.SoftwarePackageConstant.STORAGE_SOFTWARE_PACKAGE;

@InterceptorForService("softwarePackage")
public class SoftwarePackageApiInterceptor implements ApiMessageInterceptor {
    private static final List<String> VM_UPLOAD_URL_SCHEMES = Arrays.asList(
            "upload", "http", "https");

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry plugins;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIGetDirectoryUsageMsg) {
            validate((APIGetDirectoryUsageMsg) msg);
        } else if (msg instanceof APICleanSoftwarePackageMsg) {
            validate((APICleanSoftwarePackageMsg) msg);
        } else if (msg instanceof APIUploadSoftwarePackageMsg) {
            validate((APIUploadSoftwarePackageMsg) msg);
        } else if (msg instanceof APIUploadSoftwarePackageToVmMsg) {
            validate((APIUploadSoftwarePackageToVmMsg) msg);
        } else if (msg instanceof APIInstallSoftwarePackageMsg) {
            validate((APIInstallSoftwarePackageMsg) msg);
        } else if (msg instanceof APIUninstallSoftwarePackageMsg) {
            validate((APIUninstallSoftwarePackageMsg) msg);
        } else if (msg instanceof APIUploadAndExecuteSoftwareUpgradePackageMsg) {
            validate((APIUploadAndExecuteSoftwareUpgradePackageMsg) msg);
        } else if (msg instanceof APIUploadSoftwarePackageToBackupStorageMsg) {
            validate((APIUploadSoftwarePackageToBackupStorageMsg) msg);
        }
        return msg;
    }

    private void validate(APIGetDirectoryUsageMsg msg) {
        validatePath(msg.getDirectoryPath());

        if (!Objects.equals(Platform.getManagementServerId(), msg.getManagementNodeUuid())) {
            bus.makeServiceIdByManagementNodeId(msg, SoftwarePackageConstant.SERVICE_ID, msg.getManagementNodeUuid());
        }
    }

    private void validate(APICleanSoftwarePackageMsg msg) {
        SoftwarePackageVO softwarePackageVO = Q.New(SoftwarePackageVO.class)
                .eq(SoftwarePackageVO_.uuid, msg.getUuid())
                .find();
        if (softwarePackageVO == null) {
            throw new ApiMessageInterceptionException(operr("software package [%s] not found", msg.getUuid()));
        }
        msg.setSoftwarePackageVO(softwarePackageVO);

        if (!Objects.equals(softwarePackageVO.getType(), STORAGE_SOFTWARE_PACKAGE)) {
            return;
        }

        if (!Objects.equals(Platform.getManagementServerId(), softwarePackageVO.getManagementNodeUuid())) {
            bus.makeServiceIdByManagementNodeId(msg, SoftwarePackageConstant.SERVICE_ID, softwarePackageVO.getManagementNodeUuid());
        }
    }

    private void validate(APIUploadSoftwarePackageMsg msg) {
        validatePath(msg.getInstallPath());
        if (msg.getManagementNodeUuid() != null && !Objects.equals(Platform.getManagementServerId(), msg.getManagementNodeUuid())) {
            bus.makeServiceIdByManagementNodeId(msg, SoftwarePackageConstant.SERVICE_ID, msg.getManagementNodeUuid());
        }
    }

    private void validate(APIUploadSoftwarePackageToVmMsg msg) {
        try {
            URI uri = new URI(msg.getUrl());
            String scheme = uri.getScheme();
            if (scheme == null || !VM_UPLOAD_URL_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
                throw new ApiMessageInterceptionException(argerr(
                        "unsupported software package URL scheme, only upload, http and https are allowed"));
            }

            String path = uri.getPath();
            String fileName = null;
            if (path != null && !path.isEmpty() && !path.endsWith("/")) {
                fileName = path.substring(path.lastIndexOf('/') + 1);
            } else if ("upload".equalsIgnoreCase(scheme)) {
                fileName = uri.getAuthority();
            }
            if (fileName == null || !fileName.matches("[A-Za-z0-9._-]+") ||
                    ".".equals(fileName) || "..".equals(fileName)) {
                throw new ApiMessageInterceptionException(argerr(
                        "software package URL must contain a valid file name"));
            }
        } catch (URISyntaxException | NullPointerException e) {
            throw new ApiMessageInterceptionException(argerr("invalid software package URL"));
        }

        VmInstanceVO vm = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .find();
        if (vm == null) {
            throw new ApiMessageInterceptionException(operr(
                    "VM instance[uuid:%s] not found", msg.getVmInstanceUuid()));
        }
        if (!KVMConstant.KVM_HYPERVISOR_TYPE.equals(vm.getHypervisorType())) {
            throw new ApiMessageInterceptionException(operr(
                    "VM instance[uuid:%s] is not a KVM VM", msg.getVmInstanceUuid()));
        }
        if (vm.getState() != VmInstanceState.Running) {
            throw new ApiMessageInterceptionException(operr(
                    "VM instance[uuid:%s] must be Running to receive a software package, current state is %s",
                    msg.getVmInstanceUuid(), vm.getState()));
        }
        if (vm.getDefaultL3NetworkUuid() == null) {
            throw new ApiMessageInterceptionException(operr(
                    "VM instance[uuid:%s] has no default L3 network", msg.getVmInstanceUuid()));
        }
        String ip = Q.New(VmNicVO.class)
                .eq(VmNicVO_.vmInstanceUuid, vm.getUuid())
                .eq(VmNicVO_.l3NetworkUuid, vm.getDefaultL3NetworkUuid())
                .select(VmNicVO_.ip)
                .findValue();
        if (ip == null) {
            throw new ApiMessageInterceptionException(operr(
                    "VM instance[uuid:%s] has no IP on its default L3 network", msg.getVmInstanceUuid()));
        }
        msg.setHostUuid(vm.getHostUuid());
        msg.setTargetIp(ip);

        UploadSoftwarePackageToVmBackend backend = plugins.getExtensionFromMap(
                msg.getType(), UploadSoftwarePackageToVmBackend.class);
        if (backend == null) {
            throw new ApiMessageInterceptionException(operr(
                    "cannot find UploadSoftwarePackageToVmBackend for type[%s]", msg.getType()));
        }

        ErrorCode error = backend.validateTargetVm(VmInstanceInventory.valueOf(vm));
        if (error != null) {
            throw new ApiMessageInterceptionException(error);
        }

        UploadSoftwarePackageToVmSpec spec = backend.getUploadSpec(Platform.getUuid());
        if (spec == null || spec.getTargetPath() == null ||
                !spec.getTargetPath().matches("^/(root|tmp)/(?!\\.{1,2}$)[A-Za-z0-9._-]+$")) {
            throw new ApiMessageInterceptionException(operr(
                    "upload target path must be a file directly under /root or /tmp"));
        }
        if (spec.getUsername() == null || spec.getUsername().isEmpty() ||
                spec.getSshPort() < 1 || spec.getSshPort() > 65535 || spec.getPassword() == null) {
            throw new ApiMessageInterceptionException(operr("upload SSH credentials are invalid"));
        }
    }

    private void validatePath(String path) {
        if (!ShellCommandUtils.isValidPath(path)) {
            throw new ApiMessageInterceptionException(argerr("Invalid install path detected: %s. " +
                    "Paths must only contain letters, numbers, underscores, dashes, dots and slashes. " +
                    "Path traversal sequences (.. and //) are not allowed. " +
                    "Path must be absolute, must not be root, and must not target system directories.", path));
        }
    }

    private void validate(APIInstallSoftwarePackageMsg msg) {
        SoftwarePackageVO softwarePackageVO = Q.New(SoftwarePackageVO.class).eq(SoftwarePackageVO_.uuid, msg.getUuid()).find();
        if (softwarePackageVO == null) {
            throw new ApiMessageInterceptionException(operr("software package [%s] not found", msg.getUuid()));
        }
        msg.setSoftwarePackageVO(softwarePackageVO);
        if (!Objects.equals(softwarePackageVO.getStatus(), SoftwarePackageStatus.Uploaded.toString())
                && !Objects.equals(softwarePackageVO.getStatus(), SoftwarePackageStatus.InstallFailed.toString())) {
            throw new ApiMessageInterceptionException(operr("software package [%s] cannot be installed in current state [%s]. Allowed states: %s or %s.",
                    softwarePackageVO.getName(),
                    softwarePackageVO.getStatus(),
                    SoftwarePackageStatus.Uploaded.toString(),
                    SoftwarePackageStatus.InstallFailed.toString()));
        }

        if (softwarePackageVO.getManagementNodeUuid() != null && !Objects.equals(Platform.getManagementServerId(), softwarePackageVO.getManagementNodeUuid())) {
            bus.makeServiceIdByManagementNodeId(msg, SoftwarePackageConstant.SERVICE_ID, softwarePackageVO.getManagementNodeUuid());
        }
    }

    private void validate(APIUninstallSoftwarePackageMsg msg) {
        SoftwarePackageVO softwarePackageVO = Q.New(SoftwarePackageVO.class).eq(SoftwarePackageVO_.uuid, msg.getUuid()).find();
        if (softwarePackageVO == null) {
            throw new ApiMessageInterceptionException(operr("software package [%s] not found", msg.getUuid()));
        }
        msg.setSoftwarePackageVO(softwarePackageVO);

        List<String> allowedStatus = Arrays.asList(
                SoftwarePackageStatus.Installed.toString(), SoftwarePackageStatus.InstallFailed.toString(),
                SoftwarePackageStatus.Initialized.toString(), SoftwarePackageStatus.InitializeFailed.toString(),
                SoftwarePackageStatus.Upgraded.toString(), SoftwarePackageStatus.UpgradePackageUploaded.toString(),
                SoftwarePackageStatus.UpgradePackageUploadFailed.toString(),
                SoftwarePackageStatus.UpgradeExecuteFailed.toString());
        if (!allowedStatus.contains(softwarePackageVO.getStatus())) {
            throw new ApiMessageInterceptionException(operr("software package [%s] cannot be uninstalled in current state [%s]. Allowed states: %s.",
                    softwarePackageVO.getName(), softwarePackageVO.getStatus(), allowedStatus));
        }

        if (softwarePackageVO.getManagementNodeUuid() != null && !Objects.equals(Platform.getManagementServerId(), softwarePackageVO.getManagementNodeUuid())) {
            bus.makeServiceIdByManagementNodeId(msg, SoftwarePackageConstant.SERVICE_ID, softwarePackageVO.getManagementNodeUuid());
        }
    }

    private void validate(APIUploadSoftwarePackageToBackupStorageMsg msg) {
        validatePath(msg.getInstallPath());
        if (msg.getBackupStorageUuid() != null) {
            checkBackupStorageStatusAndState(msg.getBackupStorageUuid());
            checkBackupStorageNotOnlyForBackup(msg.getBackupStorageUuid());
        }
    }

    private void checkBackupStorageNotOnlyForBackup(String bsUuid) {
        if (SoftwarePackageBackupStorageUtils.isOnlyForBackupTagged(bsUuid)) {
            throw new ApiMessageInterceptionException(argerr(
                    "backup storage [uuid:%s] is tagged [onlybackup] and is reserved for backup workloads only; it cannot host software packages",
                    bsUuid));
        }
    }

    private void checkBackupStorageStatusAndState(String bsUuid) {
        Tuple tuple = Q.New(BackupStorageVO.class)
                .eq(BackupStorageVO_.uuid, bsUuid)
                .select(BackupStorageVO_.state, BackupStorageVO_.status)
                .findTuple();

        if (tuple == null) {
            throw new ApiMessageInterceptionException(operr("can not find backup storage [%s]", bsUuid));
        }

        BackupStorageState bsState = tuple.get(0, BackupStorageState.class);
        BackupStorageStatus bsStatus = tuple.get(1, BackupStorageStatus.class);

        if (bsState == BackupStorageState.Enabled && bsStatus == BackupStorageStatus.Connected) {
            return;
        }

        throw new ApiMessageInterceptionException(argerr(
                "backup storage [%s] is not state=Enabled and status=Connected, current state: %s and status: %s", bsUuid, bsState, bsStatus));
    }

    private void validate(APIUploadAndExecuteSoftwareUpgradePackageMsg msg) {
        String upgradeType = msg.getUpgradeType();

        if (!Objects.equals(upgradeType, UpgradeType.Normal.toString())
                && !Objects.equals(upgradeType, UpgradeType.Reexecute.toString())) {
            throw new ApiMessageInterceptionException(argerr(
                    "invalid upgradeType [%s], valid values: %s",
                    upgradeType, Arrays.asList(UpgradeType.values())));
        }

        if (Objects.equals(upgradeType, UpgradeType.Reexecute.toString())) {
            if (msg.getInstallPath() != null || msg.getBackupStorageUuid() != null || msg.getUrl() != null) {
                throw new ApiMessageInterceptionException(argerr("installPath, backupStorageUuid" +
                        " and url are not allowed when upgradeType is Reexecute"));
            }
            return;
        }

        // upgradeType is Normal
        if (msg.getInstallPath() == null) {
            throw new ApiMessageInterceptionException(argerr("installPath must be set when upgradeType is Normal"));
        }

        validatePath(msg.getInstallPath());
        if (msg.getBackupStorageUuid() != null) {
            checkBackupStorageStatusAndState(msg.getBackupStorageUuid());
            checkBackupStorageNotOnlyForBackup(msg.getBackupStorageUuid());
            checkUpgradeBackupStorageInOriginalZone(msg.getUuid(), msg.getBackupStorageUuid());
        }
    }

    private void checkUpgradeBackupStorageInOriginalZone(String softwarePackageUuid, String candidateBsUuid) {
        String originalBsUuid;
        try {
            originalBsUuid = SoftwarePackageBackupStorageUtils.requireOriginalBackupStorageUuid(softwarePackageUuid);
        } catch (OperationFailureException e) {
            throw new ApiMessageInterceptionException(e.getErrorCode());
        }

        List<String> originalZoneUuids = SoftwarePackageBackupStorageUtils.getZoneUuidsOfBackupStorage(originalBsUuid);
        if (originalZoneUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(operr(
                    "the backup storage [uuid:%s] hosting software package [uuid:%s] is not attached to any zone",
                    originalBsUuid, softwarePackageUuid));
        }

        List<String> candidateZoneUuids = SoftwarePackageBackupStorageUtils.getZoneUuidsOfBackupStorage(candidateBsUuid);
        if (candidateZoneUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr(
                    "backup storage [uuid:%s] is not attached to any zone;  please attach it before using as upgrade target",
                    candidateBsUuid));
        }
        if (candidateZoneUuids.stream().noneMatch(originalZoneUuids::contains)) {
            throw new ApiMessageInterceptionException(argerr(
                    "backup storage [uuid:%s] does not share any zone with the original backup storage [uuid:%s]" +
                            " hosting software package [uuid:%s]; pick a backup storage attached to one of zones %s",
                    candidateBsUuid, originalBsUuid, softwarePackageUuid, originalZoneUuids));
        }
    }
}

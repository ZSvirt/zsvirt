package org.zstack.crypto.keyprovider;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO_;
import org.zstack.header.keyprovider.KeyProviderConstant;
import org.zstack.header.keyprovider.KeyProviderGlobalConfig;
import org.zstack.header.keyprovider.KeyProviderType;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.keyprovider.KeyProviderVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO_;
import org.zstack.header.tpm.api.APIAddTpmMsg;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.vm.APICloneVmInstanceMsg;
import org.zstack.header.storage.volume.backup.APICreateVmFromVmBackupMsg;
import org.zstack.ovf.api.APICreateVmInstanceFromOvfMsg;
import org.zstack.ovf.message.CreateVmInstanceFromOvfMsg;
import org.zstack.header.vm.APICreateVmInstanceFromTemplatedVmInstanceMsg;
import org.zstack.header.vm.APICreateVmInstanceFromVolumeMsg;
import org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotGroupMsg;
import org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotMsg;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.APIMigrateVmMsg;
import org.zstack.header.vm.APIUpdateVmInstanceMsg;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.devices.VmDevicesSpec;
import org.zstack.header.volume.APIAttachDataVolumeToHostMsg;
import org.zstack.header.volume.APIAttachDataVolumeToVmMsg;
import org.zstack.header.volume.APIChangeVolumeEncryptionMsg;
import org.zstack.header.volume.APICreateDataVolumeFromVolumeSnapshotMsg;
import org.zstack.header.volume.APICreateDataVolumeFromVolumeTemplateMsg;
import org.zstack.header.volume.APICreateVolumeSnapshotGroupMsg;
import org.zstack.header.volume.APICreateVolumeSnapshotMsg;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVmMsg;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

public class KeyProviderAvailabilityApiInterceptor implements GlobalApiMessageInterceptor {
    private static final String KEY_PROVIDER_UNAVAILABLE = "keyProviderUnavailable";
    private static final String VOLUME_IMAGE_SCHEMA = "volume://";

    @Autowired
    private CloudBus bus;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVmInstanceMsg) {
            validate((APICreateVmInstanceMsg) msg);
        } else if (msg instanceof APIUpdateVmInstanceMsg) {
            validate((APIUpdateVmInstanceMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromVolumeMsg) {
            validate((APICreateVmInstanceFromVolumeMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromVolumeSnapshotMsg) {
            validate((APICreateVmInstanceFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromVolumeSnapshotGroupMsg) {
            validate((APICreateVmInstanceFromVolumeSnapshotGroupMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromTemplatedVmInstanceMsg) {
            validate((APICreateVmInstanceFromTemplatedVmInstanceMsg) msg);
        } else if (msg instanceof APICloneVmInstanceMsg) {
            validate((APICloneVmInstanceMsg) msg);
        } else if (msg instanceof APICreateVmFromVmBackupMsg) {
            validate((APICreateVmFromVmBackupMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromOvfMsg) {
            validate((APICreateVmInstanceFromOvfMsg) msg);
        } else if (msg instanceof APIMigrateVmMsg) {
            validate((APIMigrateVmMsg) msg);
        } else if (msg instanceof APIPrimaryStorageMigrateVmMsg) {
            validate((APIPrimaryStorageMigrateVmMsg) msg);
        } else if (msg instanceof APIAttachDataVolumeToVmMsg) {
            validate((APIAttachDataVolumeToVmMsg) msg);
        } else if (msg instanceof APIAttachDataVolumeToHostMsg) {
            validate((APIAttachDataVolumeToHostMsg) msg);
        } else if (msg instanceof APIChangeVolumeEncryptionMsg) {
            validate((APIChangeVolumeEncryptionMsg) msg);
        } else if (msg instanceof APICreateVolumeSnapshotMsg) {
            validate((APICreateVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICreateVolumeSnapshotGroupMsg) {
            validate((APICreateVolumeSnapshotGroupMsg) msg);
        } else if (msg instanceof APICreateDataVolumeFromVolumeSnapshotMsg) {
            validate((APICreateDataVolumeFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICreateDataVolumeFromVolumeTemplateMsg) {
            validate((APICreateDataVolumeFromVolumeTemplateMsg) msg);
        } else if (msg instanceof APIAddTpmMsg) {
            validate((APIAddTpmMsg) msg);
        }

        return msg;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return list(
                APICreateVmInstanceMsg.class,
                APIUpdateVmInstanceMsg.class,
                APICreateVmInstanceFromVolumeMsg.class,
                APICreateVmInstanceFromVolumeSnapshotMsg.class,
                APICreateVmInstanceFromVolumeSnapshotGroupMsg.class,
                APICreateVmInstanceFromTemplatedVmInstanceMsg.class,
                APICloneVmInstanceMsg.class,
                APICreateVmFromVmBackupMsg.class,
                APICreateVmInstanceFromOvfMsg.class,
                APIMigrateVmMsg.class,
                APIPrimaryStorageMigrateVmMsg.class,
                APIAttachDataVolumeToVmMsg.class,
                APIAttachDataVolumeToHostMsg.class,
                APIChangeVolumeEncryptionMsg.class,
                APICreateVolumeSnapshotMsg.class,
                APICreateVolumeSnapshotGroupMsg.class,
                APICreateDataVolumeFromVolumeSnapshotMsg.class,
                APICreateDataVolumeFromVolumeTemplateMsg.class,
                APIAddTpmMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    private void validate(APICreateVmInstanceMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectProviders(msg, providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APIUpdateVmInstanceMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectProviders(msg, providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APICreateVmInstanceFromVolumeMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectProviders(msg, providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APICreateVmInstanceFromVolumeSnapshotMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectProviders(msg, providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APICreateVmInstanceFromVolumeSnapshotGroupMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectProviders(msg, providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APICreateVmInstanceFromTemplatedVmInstanceMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectProviders(msg, providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APICloneVmInstanceMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectProviders(msg, providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APICreateVmFromVmBackupMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectProviders(msg, providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APICreateVmInstanceFromOvfMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectProviders(msg, providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APIMigrateVmMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectVmResourceProviders(msg.getVmInstanceUuid(), providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APIPrimaryStorageMigrateVmMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectVmResourceProviders(msg.getVmInstanceUuid(), providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APIAttachDataVolumeToVmMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectEncryptedVolumeProvider(msg.getVolumeUuid(), providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APIAttachDataVolumeToHostMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectEncryptedVolumeProvider(msg.getVolumeUuid(), providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APIChangeVolumeEncryptionMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectProviders(msg, providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APICreateVolumeSnapshotMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectEncryptedVolumeProvider(msg.getVolumeUuid(), providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APICreateVolumeSnapshotGroupMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectProviders(msg, providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APICreateDataVolumeFromVolumeSnapshotMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectEncryptedSnapshotProvider(msg.getVolumeSnapshotUuid(), providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APICreateDataVolumeFromVolumeTemplateMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectTemplateSourceProvider(msg.getImageUuid(), providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void validate(APIAddTpmMsg msg) {
        Set<String> providerUuids = new LinkedHashSet<>();
        collectProviders(msg, providerUuids);
        ensureProvidersAvailable(providerUuids);
    }

    private void collectProviders(APICreateVmInstanceMsg msg, Set<String> providerUuids) {
        collectTemplateSourceProvider(msg.getImageUuid(), providerUuids);
        collectRequestedTpmProvider(msg.getDevicesSpec(), providerUuids);
        collectVmEncryptionProvider(msg.getVmEncryption(), providerUuids);
        collectDiskProviders(msg.getDiskAOs(), providerUuids);
    }

    private void collectProviders(APIUpdateVmInstanceMsg msg, Set<String> providerUuids) {
        collectVmEncryptionProvider(msg.getVmEncryption(), providerUuids);
    }

    private void collectVmEncryptionProvider(Boolean vmEncryption, Set<String> providerUuids) {
        if (Boolean.TRUE.equals(vmEncryption)) {
            addDefaultProvider(providerUuids);
        }
    }

    private void collectProviders(APICreateVmInstanceFromVolumeMsg msg, Set<String> providerUuids) {
        boolean sourceEncrypted = isVolumeEncrypted(msg.getVolumeUuid());
        if (sourceEncrypted) {
            addResourceProvider(providerUuids, VolumeVO.class.getSimpleName(), msg.getVolumeUuid(), false);
        }
        collectVmEncryptionProvider(msg.getVmEncryption(), providerUuids);
    }

    private void collectProviders(APICreateVmInstanceFromVolumeSnapshotMsg msg, Set<String> providerUuids) {
        boolean sourceEncrypted = isSnapshotEncrypted(msg.getVolumeSnapshotUuid());
        if (sourceEncrypted) {
            addResourceProvider(providerUuids, VolumeSnapshotVO.class.getSimpleName(), msg.getVolumeSnapshotUuid(), false);
        }
        collectVmEncryptionProvider(msg.getVmEncryption(), providerUuids);
    }

    private void collectProviders(APICreateVmInstanceFromVolumeSnapshotGroupMsg msg, Set<String> providerUuids) {
        List<String> snapshotUuids = Q.New(VolumeSnapshotGroupRefVO.class)
                .select(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid)
                .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid, msg.getVolumeSnapshotGroupUuid())
                .listValues();

        if (snapshotUuids != null) {
            snapshotUuids.forEach(uuid -> collectEncryptedSnapshotProvider(uuid, providerUuids));
        }

        collectSnapshotGroupTpmProvider(msg.getVolumeSnapshotGroupUuid(), Boolean.TRUE.equals(msg.getResetTpm()), providerUuids);

        collectVmEncryptionProvider(msg.getVmEncryption(), providerUuids);
        if (!CollectionUtils.isEmpty(msg.getVolumeSnapshotEncryptions())) {
            for (APICreateVmInstanceFromVolumeSnapshotGroupMsg.VolumeSnapshotEncryption encryption : msg.getVolumeSnapshotEncryptions()) {
                if (Boolean.TRUE.equals(encryption.getEncrypted()) && !isSnapshotEncrypted(encryption.getVolumeSnapshotUuid())) {
                    addDefaultProvider(providerUuids);
                }
            }
        }
    }

    private void collectProviders(APICreateVmInstanceFromTemplatedVmInstanceMsg msg, Set<String> providerUuids) {
        boolean resetTpm = Boolean.TRUE.equals(msg.getResetTpm());
        collectVmResourceProviders(msg.getTemplatedVmInstanceUuid(), providerUuids, !resetTpm);
        collectVmEncryptionProvider(msg.getVmEncryption(), providerUuids);
        collectDiskProviders(msg.getDiskAOs(), providerUuids);

        if (resetTpm && hasVmTpmKeyRef(msg.getTemplatedVmInstanceUuid())) {
            addDefaultProvider(providerUuids);
        }
    }

    private void collectProviders(APICloneVmInstanceMsg msg, Set<String> providerUuids) {
        boolean resetTpm = Boolean.TRUE.equals(msg.getResetTpm());
        collectVmResourceProviders(msg.getVmInstanceUuid(), providerUuids, !resetTpm);
        collectVmEncryptionProvider(msg.getVmEncryption(), providerUuids);
        collectDiskProviders(msg.getDiskAOs(), providerUuids);

        if (resetTpm && hasVmTpmKeyRef(msg.getVmInstanceUuid())) {
            addDefaultProvider(providerUuids);
        }
    }

    private void collectProviders(APICreateVmFromVmBackupMsg msg, Set<String> providerUuids) {
        collectVmEncryptionProvider(msg.getVmEncryption(), providerUuids);
    }

    private void collectProviders(APICreateVmInstanceFromOvfMsg msg, Set<String> providerUuids) {
        CreateVmInstanceFromOvfMsg.resolveCreateVmMsg(msg);
        CreateVmInstanceFromOvfMsg.resolveOvfDiskEncryption(msg);
        Boolean vmEncryption = msg.getVmEncryption();
        if (vmEncryption == null && msg.getCreateVmMsg() != null) {
            vmEncryption = msg.getCreateVmMsg().getVmEncryption();
        }
        collectVmEncryptionProvider(vmEncryption, providerUuids);
        if (Boolean.TRUE.equals(msg.getOvfDiskEncryption())) {
            addDefaultProvider(providerUuids);
        }
    }

    private void collectProviders(APIChangeVolumeEncryptionMsg msg, Set<String> providerUuids) {
        boolean sourceEncrypted = isVolumeEncrypted(msg.getUuid());
        if (sourceEncrypted == msg.isEncrypted()) {
            return;
        }

        if (sourceEncrypted) {
            addResourceProvider(providerUuids, VolumeVO.class.getSimpleName(), msg.getUuid(), false);
        } else {
            addResourceProvider(providerUuids, VolumeVO.class.getSimpleName(), msg.getUuid(), true);
        }
    }

    private void collectProviders(APICreateVolumeSnapshotGroupMsg msg, Set<String> providerUuids) {
        String vmInstanceUuid = Q.New(VolumeVO.class)
                .select(VolumeVO_.vmInstanceUuid)
                .eq(VolumeVO_.uuid, msg.getRootVolumeUuid())
                .findValue();
        if (StringUtils.isNotBlank(vmInstanceUuid)) {
            collectVmVolumeProviders(vmInstanceUuid, providerUuids);
        } else {
            collectEncryptedVolumeProvider(msg.getRootVolumeUuid(), providerUuids);
        }
    }

    private void collectProviders(APIAddTpmMsg msg, Set<String> providerUuids) {
        addRequestedTpmProvider(msg.getKeyProviderUuid(), providerUuids);
    }

    private void collectDiskProviders(List<DiskAO> disks, Set<String> providerUuids) {
        if (CollectionUtils.isEmpty(disks)) {
            return;
        }

        for (DiskAO disk : disks) {
            if (Boolean.TRUE.equals(disk.getEncrypted())) {
                addDefaultProvider(providerUuids);
            }

            if (StringUtils.isNotBlank(disk.getTemplateUuid())) {
                collectTemplateSourceProvider(disk.getTemplateUuid(), providerUuids);
            }

            if (StringUtils.isBlank(disk.getSourceType()) || StringUtils.isBlank(disk.getSourceUuid())) {
                continue;
            }

            if (VolumeVO.class.getSimpleName().equals(disk.getSourceType())) {
                collectEncryptedVolumeProvider(disk.getSourceUuid(), providerUuids);
            } else if (VolumeSnapshotVO.class.getSimpleName().equals(disk.getSourceType())) {
                collectEncryptedSnapshotProvider(disk.getSourceUuid(), providerUuids);
            }
        }
    }

    private void collectTemplateSourceProvider(String imageUuid, Set<String> providerUuids) {
        if (StringUtils.isBlank(imageUuid)) {
            return;
        }

        String imageUrl = Q.New(ImageVO.class)
                .eq(ImageVO_.uuid, imageUuid)
                .select(ImageVO_.url)
                .findValue();
        if (StringUtils.isBlank(imageUrl)) {
            return;
        }

        if (imageUrl.startsWith(VOLUME_IMAGE_SCHEMA)) {
            String volumeUuid = imageUrl.substring(VOLUME_IMAGE_SCHEMA.length());
            collectEncryptedVolumeProvider(volumeUuid, providerUuids);
            return;
        }

        String snapshotUuid;
        if (imageUrl.startsWith(ImageConstant.IMAGE_FROM_SNAPSHOT_SCHEMA)) {
            snapshotUuid = imageUrl.substring(ImageConstant.IMAGE_FROM_SNAPSHOT_SCHEMA.length());
        } else if (imageUrl.startsWith(ImageConstant.SNAPSHOT_REUSE_IMAGE_SCHEMA)) {
            snapshotUuid = imageUrl.substring(ImageConstant.SNAPSHOT_REUSE_IMAGE_SCHEMA.length());
        } else {
            return;
        }

        snapshotUuid = snapshotUuid.length() >= 32 ? snapshotUuid.substring(0, 32) : snapshotUuid;
        collectEncryptedSnapshotProvider(snapshotUuid, providerUuids);
    }

    private void collectVmResourceProviders(String vmInstanceUuid, Set<String> providerUuids) {
        collectVmResourceProviders(vmInstanceUuid, providerUuids, true);
    }

    private void collectVmResourceProviders(String vmInstanceUuid, Set<String> providerUuids, boolean includeTpm) {
        if (StringUtils.isBlank(vmInstanceUuid)) {
            return;
        }

        if (isVmEncrypted(vmInstanceUuid)) {
            addResourceProvider(providerUuids, VmInstanceVO.class.getSimpleName(), vmInstanceUuid, true);
        }

        collectVmVolumeProviders(vmInstanceUuid, providerUuids);
        if (includeTpm) {
            collectVmTpmProvider(vmInstanceUuid, providerUuids);
        }
    }

    private void collectVmVolumeProviders(String vmInstanceUuid, Set<String> providerUuids) {
        List<String> encryptedVolumeUuids = Q.New(VolumeVO.class)
                .select(VolumeVO_.uuid)
                .eq(VolumeVO_.vmInstanceUuid, vmInstanceUuid)
                .eq(VolumeVO_.encrypted, true)
                .listValues();
        if (encryptedVolumeUuids != null) {
            encryptedVolumeUuids.forEach(uuid -> addResourceProvider(providerUuids, VolumeVO.class.getSimpleName(), uuid, false));
        }
    }

    private void collectVmTpmProvider(String vmInstanceUuid, Set<String> providerUuids) {
        if (VmGlobalConfig.ALLOWED_TPM_VM_WITHOUT_KMS.value(Boolean.class)) {
            return;
        }

        String tpmUuid = Q.New(TpmVO.class)
                .select(TpmVO_.uuid)
                .eq(TpmVO_.vmInstanceUuid, vmInstanceUuid)
                .findValue();
        if (StringUtils.isNotBlank(tpmUuid)) {
            addExistingResourceProvider(providerUuids, TpmVO.class.getSimpleName(), tpmUuid);
        }
    }

    private void collectSnapshotGroupTpmProvider(String snapshotGroupUuid, boolean resetTpm, Set<String> providerUuids) {
        String tpmBackupFileUuid = Q.New(VmHostBackupFileVO.class)
                .select(VmHostBackupFileVO_.uuid)
                .eq(VmHostBackupFileVO_.resourceUuid, snapshotGroupUuid)
                .eq(VmHostBackupFileVO_.type, VmHostFileType.TpmState)
                .findValue();
        if (StringUtils.isBlank(tpmBackupFileUuid)) {
            return;
        }

        if (resetTpm) {
            addRequestedTpmProvider(null, providerUuids);
            return;
        }

        if (VmGlobalConfig.ALLOWED_TPM_VM_WITHOUT_KMS.value(Boolean.class)) {
            return;
        }

        EncryptedResourceKeyRefVO ref = findResourceKeyRef(VmHostBackupFileVO.class.getSimpleName(), tpmBackupFileUuid);
        if (ref != null) {
            addProviderFromRef(providerUuids, ref, VmHostBackupFileVO.class.getSimpleName(), tpmBackupFileUuid);
            return;
        }

        String providerName = KVMSystemTags.TPM_KEY_PROVIDER_NAME
                .getTokenByResourceUuid(tpmBackupFileUuid, KVMSystemTags.TPM_KEY_PROVIDER_NAME_TOKEN);
        if (StringUtils.isNotBlank(providerName)) {
            String providerUuid = findProviderUuidByName(providerName);
            if (StringUtils.isNotBlank(providerUuid)) {
                providerUuids.add(providerUuid);
                return;
            }
        }

        addDefaultProvider(providerUuids);
    }

    private void collectRequestedTpmProvider(VmDevicesSpec devicesSpec, Set<String> providerUuids) {
        if (devicesSpec == null || devicesSpec.getTpm() == null || !devicesSpec.getTpm().isEnable()) {
            return;
        }

        addRequestedTpmProvider(devicesSpec.getTpm().getKeyProviderUuid(), providerUuids);
    }

    private void addRequestedTpmProvider(String keyProviderUuid, Set<String> providerUuids) {
        if (StringUtils.isBlank(keyProviderUuid) && VmGlobalConfig.ALLOWED_TPM_VM_WITHOUT_KMS.value(Boolean.class)) {
            return;
        }

        if (StringUtils.isBlank(keyProviderUuid)) {
            addDefaultProvider(providerUuids);
        } else {
            providerUuids.add(keyProviderUuid);
        }
    }

    private void collectEncryptedVolumeProvider(String volumeUuid, Set<String> providerUuids) {
        if (StringUtils.isNotBlank(volumeUuid) && isVolumeEncrypted(volumeUuid)) {
            addResourceProvider(providerUuids, VolumeVO.class.getSimpleName(), volumeUuid, false);
        }
    }

    private void collectEncryptedSnapshotProvider(String snapshotUuid, Set<String> providerUuids) {
        if (StringUtils.isNotBlank(snapshotUuid) && isSnapshotEncrypted(snapshotUuid)) {
            addResourceProvider(providerUuids, VolumeSnapshotVO.class.getSimpleName(), snapshotUuid, false);
        }
    }

    private void addResourceProvider(Set<String> providerUuids, String resourceType, String resourceUuid, boolean fallbackToDefault) {
        EncryptedResourceKeyRefVO ref = findResourceKeyRef(resourceType, resourceUuid);

        if (ref != null) {
            addProviderFromRef(providerUuids, ref, resourceType, resourceUuid);
        } else if (fallbackToDefault) {
            addDefaultProvider(providerUuids);
        } else {
            throw new ApiMessageInterceptionException(operr("cannot find key provider for encrypted resource[type:%s, uuid:%s]",
                    resourceType, resourceUuid).withOpaque(KEY_PROVIDER_UNAVAILABLE, true));
        }
    }

    private void addExistingResourceProvider(Set<String> providerUuids, String resourceType, String resourceUuid) {
        EncryptedResourceKeyRefVO ref = findResourceKeyRef(resourceType, resourceUuid);
        if (ref == null) {
            return;
        }

        addProviderFromRef(providerUuids, ref, resourceType, resourceUuid);
    }

    private void addProviderFromRef(Set<String> providerUuids, EncryptedResourceKeyRefVO ref,
                                    String resourceType, String resourceUuid) {
        String providerUuid = ref.getProviderUuid();
        if (StringUtils.isBlank(providerUuid) && StringUtils.isNotBlank(ref.getProviderName())) {
            providerUuid = findProviderUuidByName(ref.getProviderName());
        }
        if (StringUtils.isNotBlank(providerUuid)) {
            providerUuids.add(providerUuid);
        } else {
            throw new ApiMessageInterceptionException(operr("cannot find key provider for encrypted resource[type:%s, uuid:%s]",
                    resourceType, resourceUuid).withOpaque(KEY_PROVIDER_UNAVAILABLE, true));
        }
    }

    private EncryptedResourceKeyRefVO findResourceKeyRef(String resourceType, String resourceUuid) {
        return Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, resourceType)
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, resourceUuid)
                .find();
    }

    private String findProviderUuidByName(String providerName) {
        return Q.New(KeyProviderVO.class)
                .select(KeyProviderVO_.uuid)
                .eq(KeyProviderVO_.name, providerName)
                .findValue();
    }

    private boolean hasVmTpmKeyRef(String vmInstanceUuid) {
        if (StringUtils.isBlank(vmInstanceUuid)) {
            return false;
        }

        String tpmUuid = Q.New(TpmVO.class)
                .select(TpmVO_.uuid)
                .eq(TpmVO_.vmInstanceUuid, vmInstanceUuid)
                .findValue();
        return StringUtils.isNotBlank(tpmUuid) && findResourceKeyRef(TpmVO.class.getSimpleName(), tpmUuid) != null;
    }

    private void addDefaultProvider(Set<String> providerUuids) {
        String providerUuid = KeyProviderGlobalConfig.DEFAULT_KEY_PROVIDER_UUID.value(String.class);
        if (StringUtils.isBlank(providerUuid) || KeyProviderGlobalConfig.NONE.equals(providerUuid)) {
            throw new ApiMessageInterceptionException(operr("default key provider is not configured")
                    .withOpaque(KEY_PROVIDER_UNAVAILABLE, true));
        }

        providerUuids.add(providerUuid);
    }

    private void ensureProvidersAvailable(Set<String> providerUuids) {
        for (String providerUuid : providerUuids) {
            KeyProviderType type = Q.New(KeyProviderVO.class)
                    .select(KeyProviderVO_.type)
                    .eq(KeyProviderVO_.uuid, providerUuid)
                    .findValue();
            if (type == null) {
                throw new ApiMessageInterceptionException(operr("cannot find key provider[uuid:%s]",
                        providerUuid).withOpaque(KEY_PROVIDER_UNAVAILABLE, true));
            }

            TriggerKeyProviderHealthCheckMsg msg = new TriggerKeyProviderHealthCheckMsg();
            msg.setProviderUuid(providerUuid);
            msg.setProviderType(type.toString());
            msg.setRealtime(true);
            bus.makeTargetServiceIdByResourceUuid(msg, KeyProviderConstant.SERVICE_ID, providerUuid);
            MessageReply reply = bus.call(msg);
            if (!reply.isSuccess()) {
                throw new ApiMessageInterceptionException(operr("key provider[uuid:%s] realtime health check failed: %s",
                        providerUuid, reply.getError() == null ? null : reply.getError().getReadableDetails())
                        .withOpaque(KEY_PROVIDER_UNAVAILABLE, true));
            }

            TriggerKeyProviderHealthCheckReply healthCheckReply = reply.castReply();
            if (!healthCheckReply.isAvailable()) {
                throw new ApiMessageInterceptionException(operr("key provider[uuid:%s] is unavailable: %s",
                        providerUuid, healthCheckReply.getStatusMessage())
                        .withOpaque(KEY_PROVIDER_UNAVAILABLE, true));
            }
        }
    }

    private boolean isVmEncrypted(String vmInstanceUuid) {
        return Boolean.TRUE.equals(Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.vmEncryption)
                .eq(VmInstanceVO_.uuid, vmInstanceUuid)
                .findValue());
    }

    private boolean isVolumeEncrypted(String volumeUuid) {
        return Boolean.TRUE.equals(Q.New(VolumeVO.class)
                .select(VolumeVO_.encrypted)
                .eq(VolumeVO_.uuid, volumeUuid)
                .findValue());
    }

    private boolean isSnapshotEncrypted(String snapshotUuid) {
        return Boolean.TRUE.equals(Q.New(VolumeSnapshotVO.class)
                .select(VolumeSnapshotVO_.encrypted)
                .eq(VolumeSnapshotVO_.uuid, snapshotUuid)
                .findValue());
    }
}

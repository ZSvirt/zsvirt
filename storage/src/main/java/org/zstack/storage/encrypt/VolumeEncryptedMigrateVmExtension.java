package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.vm.VmMigrationType;
import org.zstack.header.vm.VmPreMigrationExtensionPoint;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.VmStateChangedExtensionPoint;
import org.zstack.header.volume.VolumeAO_;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeEncryptedMigrateVmExtension
        implements VmPreMigrationExtensionPoint, VmInstanceMigrateExtensionPoint, VmStateChangedExtensionPoint {

    private static final CLogger logger = Utils.getLogger(VolumeEncryptedMigrateVmExtension.class);
    private final Map<String, String> volumeMigratingSourceHostCache = new ConcurrentHashMap<>();

    @Autowired
    private VolumeEncryptedSecretHelper secretHelper;
    @Autowired
    private VolumeEncryptedResourceKeyBackend keyBackend;

    @Override
    public void preVmMigration(VmInstanceInventory vm, VmMigrationType type, String dstHostUuid, Completion completion) {
        if (type != VmMigrationType.HostMigration) {
            completion.success();
            return;
        }
        if (vm == null || StringUtils.isBlank(dstHostUuid)) {
            completion.success();
            return;
        }

        List<VolumeInventory> encryptedVols = collectEncryptedVolumes(vm);
        if (encryptedVols.isEmpty()) {
            completion.success();
            return;
        }

        String vmUuid = vm.getUuid();
        String srcHostUuid = getSourceHostUuid(vm, vm.getHostUuid());
        if (StringUtils.isBlank(srcHostUuid)) {
            logger.info(String.format(
                    "skip pre-defining migration LUKS secrets: source host is blank, vm[uuid:%s], dstHostUuid=%s, vm.hostUuid=%s, vm.lastHostUuid=%s",
                    vmUuid, dstHostUuid, vm.getHostUuid(), vm.getLastHostUuid()));
            completion.success();
            return;
        }

        try {
            for (VolumeInventory vol : encryptedVols) {
                secretHelper.resolveOrDefineSecretForVolumeMigration(srcHostUuid, dstHostUuid, vmUuid, vol.getUuid());
            }
            completion.success();
        } catch (Exception e) {
            completion.fail(operr("failed to pre-define LUKS secret for encrypted VM[uuid:%s] on host[uuid:%s]: %s",
                    vmUuid, dstHostUuid, e.getMessage()));
        }
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid, NoErrorCompletion completion) {
        if (inv == null) {
            logger.info("skip deleting migration LUKS secrets: vm inventory is null");
            completion.done();
            return;
        }

        String vmUuid = inv.getUuid();
        List<VolumeInventory> encryptedVolumes = collectEncryptedVolumes(inv);
        if (encryptedVolumes.isEmpty()) {
            completion.done();
            return;
        }

        String sourceHostUuid = getSourceHostUuid(inv, srcHostUuid);
        if (StringUtils.isBlank(sourceHostUuid)) {
            logger.info(String.format(
                    "skip deleting migration LUKS secrets: source host is blank, vm[uuid:%s], srcHostUuid=%s, vm.hostUuid=%s, vm.lastHostUuid=%s",
                    vmUuid, srcHostUuid, inv.getHostUuid(), inv.getLastHostUuid()));
            completion.done();
            return;
        }

        String destHostUuid = inv.getHostUuid();
        if (StringUtils.isBlank(destHostUuid) || sourceHostUuid.equals(destHostUuid)) {
            logger.info(String.format(
                    "skip deleting migration LUKS secrets: invalid host mapping, vm[uuid:%s], sourceHostUuid=%s, destHostUuid=%s, vm.lastHostUuid=%s",
                    vmUuid, sourceHostUuid, destHostUuid, inv.getLastHostUuid()));
            completion.done();
            return;
        }

        deleteEncryptedVolumeSecretsOnHost(sourceHostUuid, vmUuid, encryptedVolumes, "after-migrate");
        completion.done();
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason, NoErrorCompletion completion) {
        if (inv == null || StringUtils.isBlank(destHostUuid)) {
            logger.info(String.format(
                    "skip deleting destination LUKS secrets after failed migration: vm inventory or dest host is blank, vmUuid=%s, destHostUuid=%s",
                    inv == null ? null : inv.getUuid(), destHostUuid));
            completion.done();
            return;
        }

        String vmUuid = inv.getUuid();
        List<VolumeInventory> encryptedVolumes = collectEncryptedVolumes(inv);
        if (encryptedVolumes.isEmpty()) {
            completion.done();
            return;
        }

        deleteEncryptedVolumeSecretsOnHost(destHostUuid, vmUuid, encryptedVolumes, "failed-migrate");
        completion.done();
    }

    @Override
    public void vmStateChanged(VmInstanceInventory vm, VmInstanceState oldState, VmInstanceState newState) {
        String vmUuid = vm == null ? null : vm.getUuid();
        if (StringUtils.isBlank(vmUuid)) {
            logger.info(String.format(
                    "skip volume-migrating LUKS secret cleanup: vm uuid is blank, oldState=%s, newState=%s",
                    oldState, newState));
            return;
        }

        if (newState == VmInstanceState.VolumeMigrating) {
            List<VolumeInventory> encryptedVolumes = collectEncryptedVolumes(vm);
            if (encryptedVolumes.isEmpty()) {
                return;
            }

            String sourceHostUuid = getSourceHostUuid(vm, null);
            if (StringUtils.isBlank(sourceHostUuid)) {
                sourceHostUuid = findSecretHostFromEncryptedVolumes(encryptedVolumes);
            }
            if (StringUtils.isBlank(sourceHostUuid)) {
                logger.info(String.format(
                        "skip caching volume-migrating source host for LUKS secret cleanup: source host is blank, vm[uuid:%s], oldState=%s, newState=%s, vm.hostUuid=%s, vm.lastHostUuid=%s",
                        vmUuid, oldState, newState, vm.getHostUuid(), vm.getLastHostUuid()));
                return;
            }

            volumeMigratingSourceHostCache.put(vmUuid, sourceHostUuid);
            logger.info(String.format(
                    "cached volume-migrating source host for LUKS secret cleanup, vm[uuid:%s], oldState=%s, newState=%s, sourceHostUuid=%s, vm.hostUuid=%s, vm.lastHostUuid=%s",
                    vmUuid, oldState, newState, sourceHostUuid, vm.getHostUuid(), vm.getLastHostUuid()));
            return;
        }

        if (oldState != VmInstanceState.VolumeMigrating) {
            return;
        }

        List<VolumeInventory> encryptedVolumes = collectEncryptedVolumes(vm);
        if (encryptedVolumes.isEmpty()) {
            volumeMigratingSourceHostCache.remove(vmUuid);
            return;
        }

        String sourceHostUuid = volumeMigratingSourceHostCache.remove(vmUuid);
        if (StringUtils.isBlank(sourceHostUuid)) {
            sourceHostUuid = findSecretHostFromEncryptedVolumes(encryptedVolumes);
        }
        if (StringUtils.isBlank(sourceHostUuid)) {
            logger.info(String.format(
                    "skip volume-migrating LUKS secret cleanup: source host is blank, vm[uuid:%s], oldState=%s, newState=%s, vm.hostUuid=%s, vm.lastHostUuid=%s",
                    vmUuid, oldState, newState, vm.getHostUuid(), vm.getLastHostUuid()));
            return;
        }

        String destHostUuid = findCurrentOrLastHostUuid(vmUuid);
        if (StringUtils.isBlank(destHostUuid)) {
            destHostUuid = StringUtils.defaultIfBlank(vm.getHostUuid(), vm.getLastHostUuid());
        }
        if (StringUtils.isBlank(destHostUuid) || sourceHostUuid.equals(destHostUuid)) {
            logger.info(String.format(
                    "skip volume-migrating LUKS secret cleanup: invalid host mapping, vm[uuid:%s], oldState=%s, newState=%s, sourceHostUuid=%s, destHostUuid=%s, vm.hostUuid=%s, vm.lastHostUuid=%s",
                    vmUuid, oldState, newState, sourceHostUuid, destHostUuid, vm.getHostUuid(), vm.getLastHostUuid()));
            return;
        }

        logger.info(String.format(
                "trigger volume-migrating source LUKS secret cleanup, vm[uuid:%s], oldState=%s, newState=%s, sourceHostUuid=%s, destHostUuid=%s",
                vmUuid, oldState, newState, sourceHostUuid, destHostUuid));
        deleteEncryptedVolumeSecretsOnHost(sourceHostUuid, vmUuid, encryptedVolumes, "volume-migrated-host-change");
    }

    private String getSourceHostUuid(VmInstanceInventory inv, String srcHostUuid) {
        if (StringUtils.isNotBlank(srcHostUuid)) {
            return srcHostUuid;
        }
        return inv == null ? null : inv.getLastHostUuid();
    }

    protected String findCurrentOrLastHostUuid(String vmUuid) {
        if (StringUtils.isBlank(vmUuid)) {
            return null;
        }
        String hostUuid = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.hostUuid)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .findValue();
        if (StringUtils.isNotBlank(hostUuid)) {
            return hostUuid;
        }
        return Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.lastHostUuid)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .findValue();
    }

    private String findSecretHostFromEncryptedVolumes(List<VolumeInventory> encryptedVolumes) {
        for (VolumeInventory vol : encryptedVolumes) {
            String hostUuid = resolveSecretHostUuidFromTag(vol.getUuid());
            if (StringUtils.isNotBlank(hostUuid)) {
                return hostUuid;
            }
        }
        return null;
    }

    private String resolveSecretHostUuidFromTag(String volUuid) {
        List<String> tags = VolumeSystemTags.VOLUME_LIBVIRT_SECRET_HOST.getTags(volUuid, VolumeVO.class);
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return VolumeSystemTags.VOLUME_LIBVIRT_SECRET_HOST.getTokenByTag(
                tags.get(0), VolumeSystemTags.VOLUME_LIBVIRT_SECRET_HOST_TOKEN);
    }

    private void deleteEncryptedVolumeSecretsOnHost(String hostUuid, String vmUuid,
                                                   List<VolumeInventory> encryptedVolumes, String reason) {
        if (encryptedVolumes == null || encryptedVolumes.isEmpty()) {
            logger.info(String.format(
                    "skip LUKS secret cleanup: no encrypted volumes, host[uuid:%s], vm[uuid:%s], reason=%s",
                    hostUuid, vmUuid, reason));
            return;
        }

        for (VolumeInventory vol : encryptedVolumes) {
            Integer keyVersion = keyBackend.findKeyVersionByVolume(vol.getUuid());
            if (keyVersion == null) {
                logger.info(String.format(
                        "skip LUKS secret cleanup for encrypted volume[uuid:%s]: keyVersion is null, host[uuid:%s], vm[uuid:%s], reason=%s",
                        vol.getUuid(), hostUuid, vmUuid, reason));
                continue;
            }

            logger.info(String.format(
                    "delete LUKS libvirt secret on host[uuid:%s] for encrypted volume[uuid:%s], vm[uuid:%s], keyVersion=%s, reason=%s",
                    hostUuid, vol.getUuid(), vmUuid, keyVersion, reason));
            secretHelper.deleteSecretOnHostBestEffort(hostUuid, vmUuid, vol.getUuid(), keyVersion);
        }
    }

    protected List<VolumeInventory> collectEncryptedVolumes(VmInstanceInventory vm) {
        List<VolumeInventory> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        VolumeInventory root = vm.getRootVolume();
        if (root != null && Boolean.TRUE.equals(root.getEncrypted())) {
            result.add(root);
            seen.add(root.getUuid());
        }
        if (vm.getAllDiskVolumes() != null) {
            for (VolumeInventory v : vm.getAllDiskVolumes()) {
                if (v != null && Boolean.TRUE.equals(v.getEncrypted()) && seen.add(v.getUuid())) {
                    result.add(v);
                }
            }
        }

        List<VolumeVO> dbVolumes = Q.New(VolumeVO.class)
                .eq(VolumeAO_.vmInstanceUuid, vm.getUuid())
                .list();
        for (VolumeVO v : dbVolumes) {
            if (v.isEncrypted() && seen.add(v.getUuid())) {
                result.add(VolumeInventory.valueOf(v));
            }
        }
        return result;
    }
}

package org.zstack.crypto.volumebackup;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageBackupStorageRefVO_;
import org.zstack.header.storage.backup.VolumeBackupStatus;
import org.zstack.header.storage.backup.VolumeBackupStorageRefVO;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.storage.backup.VolumeBackupVO_;
import org.zstack.header.volume.VolumeBackupEncryptionConversionResult;
import org.zstack.storage.backup.VolumeBackupKvmCommands;
import org.zstack.storage.backup.VolumeBackupUtils;
import org.zstack.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

final class VolumeBackupEncryptionConversionHelper {
    private static final List<VolumeBackupStatus> BACKUP_CHAIN_STATUSES =
            Arrays.asList(VolumeBackupStatus.Ready, VolumeBackupStatus.Deleted);

    static boolean isBackupChainStatus(VolumeBackupStatus status) {
        return BACKUP_CHAIN_STATUSES.contains(status);
    }

    static List<VolumeBackupVO> findBackups(String volumeUuid) {
        return Q.New(VolumeBackupVO.class)
                .eq(VolumeBackupVO_.volumeUuid, volumeUuid)
                .in(VolumeBackupVO_.status, BACKUP_CHAIN_STATUSES)
                .list();
    }

    static List<BackupConversionLeaf> buildBackupLeaves(List<VolumeBackupVO> backups) {
        List<BackupConversionLeaf> leaves = new ArrayList<>();
        for (VolumeBackupVO backup : backups) {
            if (CollectionUtils.isEmpty(backup.getBackupStorageRefs())) {
                leaves.add(new BackupConversionLeaf(backup, null));
                continue;
            }
            for (VolumeBackupStorageRefVO ref : backup.getBackupStorageRefs()) {
                if (ref == null || !isBackupChainStatus(ref.getStatus()) ||
                        StringUtils.isBlank(ref.getBackupStorageUuid()) || StringUtils.isBlank(ref.getInstallPath())) {
                    throw operr("backup[uuid:%s] has a storage ref that cannot be converted", backup.getUuid())
                            .toException();
                }
                leaves.add(new BackupConversionLeaf(backup, ref));
            }
        }
        return leaves;
    }

    static List<BackupConversionGroup> buildConversionGroups(List<BackupConversionLeaf> leaves,
                                                              boolean targetEncrypted) {
        Map<String, Map<String, List<BackupConversionLeaf>>> leavesByStorageAndChain = new LinkedHashMap<>();
        List<List<BackupConversionLeaf>> chains = new ArrayList<>();
        for (BackupConversionLeaf leaf : leaves) {
            if (!leaf.hasRef()) {
                continue;
            }
            Map<String, List<BackupConversionLeaf>> leavesByChain = leavesByStorageAndChain
                    .computeIfAbsent(leaf.backupStorageUuid, ignored -> new LinkedHashMap<>());
            String chainName = VolumeBackupUtils.getBackupChainName(leaf.oldInstallPath);
            List<BackupConversionLeaf> chainLeaves = leavesByChain.get(chainName);
            if (chainLeaves == null) {
                chainLeaves = new ArrayList<>();
                leavesByChain.put(chainName, chainLeaves);
                chains.add(chainLeaves);
            }
            chainLeaves.add(leaf);
        }

        return chains.stream()
                .filter(chain -> chain.stream()
                        .anyMatch(leaf -> leaf.sourceEncrypted != targetEncrypted))
                .map(chain -> buildConversionGroup(chain.get(0).backupStorageUuid, chain))
                .collect(Collectors.toList());
    }

    private static BackupConversionGroup buildConversionGroup(String backupStorageUuid,
                                                              List<BackupConversionLeaf> chainLeaves) {
        BackupConversionGroup group = new BackupConversionGroup();
        group.backupStorageUuid = backupStorageUuid;
        group.targetChainName = Platform.getUuid();
        group.leaves = chainLeaves.stream()
                .sorted((left, right) -> Long.compare(left.refId, right.refId))
                .collect(Collectors.toList());

        Map<String, BackupConversionLeaf> ownerByPath = new LinkedHashMap<>();
        for (BackupConversionLeaf leaf : group.leaves) {
            BackupConversionLeaf old = ownerByPath.putIfAbsent(leaf.oldInstallPath, leaf);
            if (old != null && (old.refId != leaf.refId ||
                    !Objects.equals(old.backupUuid, leaf.backupUuid) ||
                    old.sourceEncrypted != leaf.sourceEncrypted)) {
                throw operr("conflicting backup ownership for backupStorage[uuid:%s], installPath[%s]",
                        backupStorageUuid, leaf.oldInstallPath).toException();
            }
        }

        group.leafInstallPaths = new ArrayList<>(ownerByPath.keySet());
        Set<String> imageInstallPaths = new LinkedHashSet<>(Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, backupStorageUuid)
                .in(ImageBackupStorageRefVO_.installPath, group.leafInstallPaths)
                .select(ImageBackupStorageRefVO_.installPath)
                .listValues());
        for (BackupConversionLeaf leaf : group.leaves) {
            leaf.deleteOld = !imageInstallPaths.contains(leaf.oldInstallPath);
        }
        return group;
    }

    static void applyConversionResult(
            BackupConversionGroup group, VolumeBackupKvmCommands.ConvertVolumeBackupResponse rsp) {
        Map<String, String> installPaths = rsp.getInstallPaths();
        if (installPaths == null) {
            throw operr("backup conversion returned no installPaths").toException();
        }
        Set<String> uniqueNew = new LinkedHashSet<>();
        for (BackupConversionLeaf leaf : group.leaves) {
            String newInstallPath = installPaths.get(leaf.oldInstallPath);
            if (StringUtils.isBlank(newInstallPath) || !uniqueNew.add(newInstallPath)) {
                throw operr("backup conversion returned empty or duplicate new installPath for oldInstallPath[%s]",
                        leaf.oldInstallPath).toException();
            }
            leaf.newInstallPath = newInstallPath;
        }
    }

    static VolumeBackupEncryptionConversionResult buildResult(BackupConversionContext context) {
        VolumeBackupEncryptionConversionResult result = new VolumeBackupEncryptionConversionResult();
        result.setVolumeUuid(context.volumeUuid);
        result.setTargetEncrypted(context.targetEncrypted);

        List<VolumeBackupEncryptionConversionResult.RefRepoint> refs = new ArrayList<>();
        Set<String> convertedBackupUuids = new LinkedHashSet<>();
        for (BackupConversionLeaf leaf : context.leavesToUpdate) {
            convertedBackupUuids.add(leaf.backupUuid);
            if (!leaf.hasRef()) {
                continue;
            }
            if (StringUtils.isBlank(leaf.newInstallPath)) {
                throw operr("missing converted installPath for backup ref[id:%s, backupStorageUuid:%s, oldInstallPath:%s]",
                        leaf.refId, leaf.backupStorageUuid, leaf.oldInstallPath).toException();
            }
            VolumeBackupEncryptionConversionResult.RefRepoint ref =
                    new VolumeBackupEncryptionConversionResult.RefRepoint();
            ref.setRefId(leaf.refId);
            ref.setBackupUuid(leaf.backupUuid);
            ref.setBackupStorageUuid(leaf.backupStorageUuid);
            ref.setOldInstallPath(leaf.oldInstallPath);
            ref.setNewInstallPath(leaf.newInstallPath);
            ref.setSourceEncrypted(leaf.sourceEncrypted);
            refs.add(ref);
        }
        result.setRefsToRepoint(refs);
        result.setConvertedBackupUuids(new ArrayList<>(convertedBackupUuids));

        List<String> placeholderBackupUuids = context.completedGroups.stream()
                .map(group -> group.placeholderBackupUuid)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        result.setPlaceholderBackupUuids(placeholderBackupUuids);
        result.setDeleteOldBits(buildDeleteOldBits(context.groups));
        return result;
    }

    private static List<VolumeBackupEncryptionConversionResult.DeleteBits> buildDeleteOldBits(
            List<BackupConversionGroup> groups) {
        List<VolumeBackupEncryptionConversionResult.DeleteBits> bitsList = new ArrayList<>();
        Map<String, Set<String>> seenInstallPaths = new LinkedHashMap<>();
        for (BackupConversionGroup group : groups) {
            Set<String> seen = seenInstallPaths.computeIfAbsent(group.backupStorageUuid,
                    ignored -> new LinkedHashSet<>());
            for (BackupConversionLeaf leaf : group.leaves) {
                if (!leaf.deleteOld || StringUtils.isBlank(leaf.newInstallPath) ||
                        !seen.add(leaf.oldInstallPath)) {
                    continue;
                }
                VolumeBackupEncryptionConversionResult.DeleteBits bits =
                        new VolumeBackupEncryptionConversionResult.DeleteBits();
                bits.setBackupStorageUuid(group.backupStorageUuid);
                bits.setInstallPath(leaf.oldInstallPath);
                bitsList.add(bits);
            }
        }
        return bitsList;
    }

}

class BackupConversionLeaf {
    final String backupUuid;
    final boolean sourceEncrypted;
    final long refId;
    final String backupStorageUuid;
    final String oldInstallPath;
    boolean deleteOld;
    String newInstallPath;

    BackupConversionLeaf(VolumeBackupVO backup, VolumeBackupStorageRefVO ref) {
        backupUuid = backup.getUuid();
        sourceEncrypted = backup.isEncrypted();
        refId = ref == null ? 0 : ref.getId();
        backupStorageUuid = ref == null ? null : ref.getBackupStorageUuid();
        oldInstallPath = ref == null ? null : ref.getInstallPath();
    }

    boolean hasRef() {
        return refId > 0 && StringUtils.isNotBlank(backupStorageUuid) &&
                StringUtils.isNotBlank(oldInstallPath);
    }
}

class BackupConversionGroup {
    final String taskUuid = Platform.getUuid();
    String backupStorageUuid;
    String targetChainName;
    List<BackupConversionLeaf> leaves;
    List<String> leafInstallPaths;
    String placeholderBackupUuid;
}

class BackupConversionContext {
    final String volumeUuid;
    final boolean targetEncrypted;
    final List<BackupConversionGroup> groups;
    final List<BackupConversionLeaf> leavesToUpdate;
    final String hostUuid;
    final String encryptedDek;
    final List<BackupConversionGroup> completedGroups = new ArrayList<>();

    BackupConversionContext(String volumeUuid, boolean targetEncrypted, List<BackupConversionGroup> groups,
                            List<BackupConversionLeaf> leavesToUpdate, String hostUuid, String encryptedDek) {
        this.volumeUuid = volumeUuid;
        this.targetEncrypted = targetEncrypted;
        this.groups = groups;
        this.leavesToUpdate = leavesToUpdate;
        this.hostUuid = hostUuid;
        this.encryptedDek = encryptedDek;
    }
}

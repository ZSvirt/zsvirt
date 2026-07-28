package org.zstack.crypto.volumebackup;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.identity.AccountManager;
import org.zstack.header.storage.backup.VolumeBackupState;
import org.zstack.header.storage.backup.VolumeBackupStatus;
import org.zstack.header.storage.backup.VolumeBackupStorageRefVO;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.storage.backup.VolumeBackupVO_;
import org.zstack.header.volume.VolumeBackupEncryptionConversionResult;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KvmCommandFailureChecker;
import org.zstack.kvm.KvmCommandSender;
import org.zstack.kvm.KvmResponseWrapper;
import org.zstack.storage.backup.VolumeBackupKvmBackend;
import org.zstack.storage.backup.VolumeBackupKvmCommands;
import org.zstack.storage.backup.VolumeBackupMetadataMaker;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageGlobalProperty;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageManager;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageSelector;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO_;
import org.zstack.storage.backup.imagestore.ImageStoreGlobalConfig;
import org.zstack.storage.encrypt.VolumeEncryptedSecretHelper;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class VolumeBackupEncryptionConversion {
    private static final CLogger logger = Utils.getLogger(VolumeBackupEncryptionConversion.class);

    @Autowired
    private VolumeEncryptedSecretHelper volumeEncryptedSecretHelper;
    @Autowired
    private ImageStoreBackupStorageManager imageStoreBackupStorageManager;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;

    public ErrorCode validate(VolumeInventory volume, boolean encrypted) {
        if (volume == null || StringUtils.isBlank(volume.getUuid())) {
            return null;
        }

        Set<String> backupStorageUuids = new LinkedHashSet<>();
        for (VolumeBackupVO backup : VolumeBackupEncryptionConversionHelper.findBackups(volume.getUuid())) {
            if (backup.isEncrypted() == encrypted || CollectionUtils.isEmpty(backup.getBackupStorageRefs())) {
                continue;
            }
            for (VolumeBackupStorageRefVO ref : backup.getBackupStorageRefs()) {
                if (ref != null && VolumeBackupEncryptionConversionHelper.isBackupChainStatus(ref.getStatus()) &&
                        StringUtils.isNotBlank(ref.getBackupStorageUuid())) {
                    backupStorageUuids.add(ref.getBackupStorageUuid());
                }
            }
        }
        if (backupStorageUuids.isEmpty()) {
            return null;
        }

        Set<String> remoteBsUuids = new LinkedHashSet<>(ImageStoreBackupStorageSelector.getRemoteBsUuids());
        remoteBsUuids.retainAll(backupStorageUuids);
        if (!remoteBsUuids.isEmpty()) {
            return operr("cannot change encryption for volume[uuid:%s]: it has backups on off-site backup storage%s," +
                            " which does not support encrypted volume backup",
                    volume.getUuid(), remoteBsUuids);
        }
        return null;
    }

    public void convert(VolumeInventory volume, boolean targetEncrypted,
                        ReturnValueCompletion<VolumeBackupEncryptionConversionResult> completion) {
        if (volume == null || StringUtils.isBlank(volume.getUuid())) {
            completion.success(null);
            return;
        }

        List<VolumeBackupVO> backups = VolumeBackupEncryptionConversionHelper.findBackups(volume.getUuid());
        if (backups.stream().noneMatch(backup -> backup.isEncrypted() != targetEncrypted)) {
            completion.success(null);
            return;
        }
        List<BackupConversionLeaf> leaves =
                VolumeBackupEncryptionConversionHelper.buildBackupLeaves(backups);
        List<BackupConversionLeaf> candidates = leaves.stream()
                .filter(leaf -> leaf.sourceEncrypted != targetEncrypted)
                .collect(Collectors.toList());
        List<BackupConversionGroup> groups =
                VolumeBackupEncryptionConversionHelper.buildConversionGroups(leaves, targetEncrypted);
        String hostUuid = null;
        String encryptedDek = null;
        if (!groups.isEmpty()) {
            hostUuid = selectBackupConversionHost(volume);
            boolean needsEncryption = targetEncrypted || groups.stream()
                    .flatMap(group -> group.leaves.stream())
                    .anyMatch(leaf -> leaf.sourceEncrypted);
            if (needsEncryption) {
                encryptedDek = volumeEncryptedSecretHelper.materializeAndSealVolumeDekForHost(
                        hostUuid, volume.getUuid());
            }
        }

        List<BackupConversionLeaf> leavesToUpdate = candidates.stream()
                .filter(leaf -> !leaf.hasRef())
                .collect(Collectors.toCollection(ArrayList::new));
        groups.stream().flatMap(group -> group.leaves.stream()).forEach(leavesToUpdate::add);

        BackupConversionContext context = new BackupConversionContext(volume.getUuid(), targetEncrypted, groups,
                leavesToUpdate, hostUuid, encryptedDek);
        convertGroups(context, completion);
    }

    public void refreshBackupMetadata(List<String> backupUuids) {
        List<VolumeBackupVO> backups = Q.New(VolumeBackupVO.class)
                .in(VolumeBackupVO_.uuid, backupUuids)
                .list();
        Map<String, List<VolumeBackupVO>> backupsByBackupStorageUuid = new LinkedHashMap<>();
        for (VolumeBackupVO backup : backups) {
            if (backup.getStatus() != VolumeBackupStatus.Ready || backup.getBackupStorageRefs() == null) {
                continue;
            }
            for (VolumeBackupStorageRefVO ref : backup.getBackupStorageRefs()) {
                if (ref.getStatus() != VolumeBackupStatus.Ready || StringUtils.isBlank(ref.getBackupStorageUuid())) {
                    continue;
                }
                backupsByBackupStorageUuid
                        .computeIfAbsent(ref.getBackupStorageUuid(), ignored -> new ArrayList<>())
                        .add(backup);
            }
        }
        VolumeBackupMetadataMaker metadataMaker = new VolumeBackupMetadataMaker();
        backupsByBackupStorageUuid.forEach((bsUuid, bsBackups) ->
                metadataMaker.updateVolumeBackupMetadata(bsBackups, bsUuid));
    }

    private void convertGroups(BackupConversionContext context,
                               ReturnValueCompletion<VolumeBackupEncryptionConversionResult> completion) {
        new While<>(context.groups).each((group, whileCompletion) -> {
            runConversion(context, group, new Completion(whileCompletion) {
                @Override
                public void success() {
                    context.completedGroups.add(group);
                    whileCompletion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    whileCompletion.addError(errorCode);
                    whileCompletion.allDone();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errors) {
                if (errors.isEmpty()) {
                    completion.success(VolumeBackupEncryptionConversionHelper.buildResult(context));
                } else {
                    rollbackConversion(context, errors.getCauses().get(0), completion);
                }
            }
        });
    }

    private void rollbackConversion(BackupConversionContext context, ErrorCode errorCode,
                                    ReturnValueCompletion<VolumeBackupEncryptionConversionResult> completion) {
        context.completedGroups.forEach(group -> deletePlaceholderBackupBestEffort(group.placeholderBackupUuid));
        completion.fail(errorCode);
    }

    private void runConversion(BackupConversionContext context, BackupConversionGroup group, Completion completion) {
        VolumeBackupKvmCommands.ConvertVolumeBackupCmd cmd;
        try {
            createPlaceholder(context, group);
            String bsAgentHost = Q.New(ImageStoreBackupStorageVO.class)
                    .select(ImageStoreBackupStorageVO_.hostname)
                    .eq(ImageStoreBackupStorageVO_.uuid, group.backupStorageUuid)
                    .findValue();
            if (StringUtils.isBlank(bsAgentHost)) {
                throw operr("cannot resolve agent host for backupStorage[uuid:%s]", group.backupStorageUuid)
                        .toException();
            }

            cmd = new VolumeBackupKvmCommands.ConvertVolumeBackupCmd();
            cmd.setTaskUuid(group.taskUuid);
            cmd.setBsAgentHost(bsAgentHost);
            cmd.setBsAgentPort(ImageStoreBackupStorageGlobalProperty.AGENT_PORT);
            cmd.setNbdHost(VolumeBackupKvmBackend.getBackupAddress(group.backupStorageUuid));
            cmd.setTargetChainName(group.targetChainName);
            cmd.setTargetEncrypted(context.targetEncrypted);
            cmd.setUploadConcurrency(ImageStoreGlobalConfig.BLOB_UPLOAD_CONCURRENCY.value(Integer.class));
            cmd.setEncryptedDek(context.encryptedDek);
            cmd.setLeafInstallPaths(group.leafInstallPaths);
        } catch (RuntimeException e) {
            failGroup(group, operr("failed to prepare backup conversion for backupStorage[uuid:%s]: %s",
                    group.backupStorageUuid, e.getMessage()), completion);
            return;
        }

        new KvmCommandSender(context.hostUuid).send(cmd,
                VolumeBackupKvmCommands.CONVERT_VOLUME_BACKUP_PATH,
                new KvmCommandFailureChecker() {
                    @Override
                    public ErrorCode getError(KvmResponseWrapper wrapper) {
                        VolumeBackupKvmCommands.ConvertVolumeBackupResponse rsp = wrapper.getResponse(
                                VolumeBackupKvmCommands.ConvertVolumeBackupResponse.class);
                        return rsp.isSuccess() ? null : operr("%s", rsp.getError());
                    }
                }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
                    @Override
                    public void success(KvmResponseWrapper wrapper) {
                        try {
                            VolumeBackupKvmCommands.ConvertVolumeBackupResponse rsp = wrapper.getResponse(
                                    VolumeBackupKvmCommands.ConvertVolumeBackupResponse.class);
                            VolumeBackupEncryptionConversionHelper.applyConversionResult(group, rsp);
                            completion.success();
                        } catch (RuntimeException e) {
                            failGroup(group,
                                    operr("invalid backup conversion result for backupStorage[uuid:%s]: %s",
                                            group.backupStorageUuid, e.getMessage()), completion);
                        }
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        failGroup(group, errorCode, completion);
                    }
                });
    }

    private void failGroup(BackupConversionGroup group, ErrorCode errorCode,
                           Completion completion) {
        deletePlaceholderBackupBestEffort(group.placeholderBackupUuid);
        completion.fail(errorCode);
    }

    private void createPlaceholder(BackupConversionContext context, BackupConversionGroup group) {
        VolumeBackupVO original = Q.New(VolumeBackupVO.class)
                .eq(VolumeBackupVO_.uuid, group.leaves.get(0).backupUuid)
                .find();
        if (original == null) {
            throw operr("cannot create backup conversion placeholder: original backup[uuid:%s] is gone",
                    group.leaves.get(0).backupUuid).toException();
        }
        String placeholderUuid = Platform.getUuid();
        String installPath = String.format("zstore://%s/*", group.targetChainName);
        String accountUuid = acntMgr.getOwnerAccountUuidOfResource(context.volumeUuid);
        new SQLBatch() {
            @Override
            protected void scripts() {
                VolumeBackupVO vbvo = new VolumeBackupVO();
                vbvo.setUuid(placeholderUuid);
                vbvo.setAccountUuid(accountUuid);
                vbvo.setType(original.getType());
                vbvo.setVolumeUuid(context.volumeUuid);
                vbvo.setName(group.targetChainName);
                vbvo.setSize(0L);
                vbvo.setState(VolumeBackupState.Disabled);
                vbvo.setStatus(VolumeBackupStatus.Creating);
                vbvo.setEncrypted(context.targetEncrypted);
                vbvo.setVmInstanceUuid(original.getVmInstanceUuid());
                dbf.persist(vbvo);

                VolumeBackupStorageRefVO ref = new VolumeBackupStorageRefVO();
                ref.setVolumeBackupUuid(placeholderUuid);
                ref.setBackupStorageUuid(group.backupStorageUuid);
                ref.setStatus(VolumeBackupStatus.Creating);
                ref.setInstallPath(installPath);
                dbf.persist(ref);
            }
        }.execute();
        group.placeholderBackupUuid = placeholderUuid;
    }

    private void deletePlaceholderBackupBestEffort(String placeholderBackupUuid) {
        if (StringUtils.isBlank(placeholderBackupUuid)) {
            return;
        }
        try {
            dbf.removeByPrimaryKey(placeholderBackupUuid, VolumeBackupVO.class);
        } catch (RuntimeException e) {
            logger.warn(String.format("failed to delete backup encryption conversion placeholder[uuid:%s]: %s",
                    placeholderBackupUuid, e.getMessage()), e);
        }
    }

    public void deleteConversionPlaceholders(List<String> placeholderBackupUuids) {
        if (CollectionUtils.isEmpty(placeholderBackupUuids)) {
            return;
        }
        placeholderBackupUuids.forEach(this::deletePlaceholderBackupBestEffort);
    }

    private String selectBackupConversionHost(VolumeInventory volume) {
        org.zstack.header.errorcode.ErrorableValue<String> host = imageStoreBackupStorageManager.findConnectedKvmHost(
                volume.getPrimaryStorageUuid(), "primary storage", "convert volume backup encryption");
        if (!host.isSuccess()) {
            throw new OperationFailureException(host.error);
        }
        return host.result;
    }

}

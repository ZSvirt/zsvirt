package org.zstack.storage.backup;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.imagestore.ImageStorageContinueConnectExtensionPoint;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.volume.backup.CreateVolumeBackupExtensionPoint;
import org.zstack.header.storage.volume.backup.DeleteVolumeBackupExtensionPoint;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.volumebackup.VolumeBackupMetadataExtensionPoint;
import org.zstack.storage.backup.imagestore.*;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.storage.backup.imagestore.ImageStoreBackupStorageCommands.AgentCommand;
import static org.zstack.storage.backup.imagestore.ImageStoreBackupStorageCommands.ImageStoreResponse;

/**
 * Created by MaJin on 2019/4/25.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeBackupMetadataMaker implements CreateVolumeBackupExtensionPoint,
        DeleteVolumeBackupExtensionPoint, ImageStorageContinueConnectExtensionPoint {
    private final static CLogger logger = Utils.getLogger(VolumeBackupMetadataMaker.class);

    public static final String MERGE_BACKUP_METADATA = "/imagestore/mergebackupmetadata/";
    public static final String DELETE_BACKUP_METADATA = "/imagestore/deletebackupmetadata/";
    public static final String CHECK_BACKUP_METADATA_FILE = "/imagestore/checkbackupmetadatafile/";

    public static final String SYNC_MERGE_BACKUP_METADATA = "/imagestore/mergebackupmetadata-sync/";
    public static final String SYNC_DELETE_BACKUP_METADATA = "/imagestore/deletebackupmetadata-sync/";
    public static final String SYNC_CHECK_BACKUP_METADATA_FILE = "/imagestore/checkbackupmetadatafile-sync/";

    private static final String METADATA_FILE_NAME = "vol_backup_info.json";

    public static class MergeVolumeBackupMetadataCmd extends AgentCommand {
        public List<String> backupMetadatas;
    }

    public static class MergeVolumeBackupMetadataRsp extends ImageStoreResponse {
    }

    public static class DeleteVolumeBackupMetadataCmd extends AgentCommand {
        public List<String> backupUuids;
    }

    public static class DeleteVolumeBackupMetadataRsp extends ImageStoreResponse {
    }

    public static class CheckBackupMetadataFileCmd extends AgentCommand {
    }

    public static class CheckBackupMetadataFileRsp extends ImageStoreResponse {
        public boolean exist;
    }

    @Autowired
    private RESTFacade restf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private AnsibleFacade asf;

    @Override
    public void afterCreateVmBackup(Collection<VolumeBackupVO> backups, String bsUuid) {
        mergeVolumeBackupMetadataToImageStore(backups, bsUuid);
    }

    @Override
    public void beforeCreateVolumeBackup(VolumeBackupVO backup, String bsUuid) {
        SystemTagCreator creator = VolumeBackupSystemTag.NO_METADATA.newSystemTagCreator(backup.getUuid());
        creator.setTagByTokens(Collections.singletonMap(VolumeBackupSystemTag.NO_METADATA_BSUUID_TOKEN, bsUuid));
        creator.inherent = false;
        creator.recreate = false;
        creator.ignoreIfExisting = true;
        creator.create();
    }

    @Override
    public void failedToCreateVolumeBackup(VolumeBackupVO backup, String bsUuid) {
        if (backup == null) {
            return;
        }

        VolumeBackupSystemTag.NO_METADATA.delete(backup.getUuid());
    }

    @Override
    public void afterCreateVolumeBackup(VolumeBackupVO backup, String bsUuid) {
        mergeVolumeBackupMetadataToImageStore(Collections.singletonList(backup), bsUuid);
    }

    @Override
    public void afterDeleteVolumeBackup(String backupUuid, List<String> bsUuids) {
        for (String bsUuid : bsUuids) {
            deleteVolumeBackupsMetadata(Collections.singletonList(backupUuid), bsUuid);
        }
    }

    @Override
    public List<Flow> createImageStoreConnectingFlow(boolean newAdded, ImageStoreBackupStorageInventory inv) {
        List<Flow> results = new ArrayList<>();
        final boolean[] fileExists = {false};
        results.add(new NoRollbackFlow() {
            String __name__ = "check-backup-metadata-file-exist";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                checkBackupMetadataFile(inv.getUuid(), new ReturnValueCompletion<Boolean>(trigger) {
                    @Override
                    public void success(Boolean exists) {
                        fileExists[0] = exists;
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        });
        results.add(new NoRollbackFlow() {
            String __name__ = "push-backup-metadata";

            @Override
            public boolean skip(Map data) {
                return fileExists[0];
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                logger.debug("backup metadata file not exist, rebuild backup in db metadata");
                try {
                    pushVolumeBackupsMetadataToImageStore(inv.getUuid());
                    logger.debug(String.format("generate volume backup metadata file on image store[uuid:%s] success!", inv.getUuid()));
                    trigger.next();
                } catch (IOException e) {
                    trigger.fail(operr("generate volume backup metadata file on image store[uuid:%s] failure, " +
                            "because IO error: %s", inv.getUuid(), e.getMessage()));
                }
            }
        });
        results.add(new NoRollbackFlow() {
            String __name__ = "async-merge-miss-vol-backup-metadata";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<VolumeBackupVO> backups = getMissMetadataVolumeBackup();
                if (!backups.isEmpty()) {
                    mergeVolumeBackupMetadataToImageStore(backups, inv.getUuid());
                }
                trigger.next();
            }

            @Transactional(readOnly = true)
            private List<VolumeBackupVO> getMissMetadataVolumeBackup() {
                String tag = VolumeBackupSystemTag.NO_METADATA.instantiateTag(
                        Collections.singletonMap(VolumeBackupSystemTag.NO_METADATA_BSUUID_TOKEN, inv.getUuid()));
                List<String> resourceUuids = Q.New(SystemTagVO.class).select(SystemTagVO_.resourceUuid)
                        .eq(SystemTagVO_.tag, tag)
                        .listValues();
                return resourceUuids.isEmpty() ? Collections.EMPTY_LIST :
                        Q.New(VolumeBackupVO.class).in(VolumeBackupVO_.uuid, resourceUuids)
                                .eq(VolumeBackupVO_.status, VolumeBackupStatus.Ready)
                                .list();
            }
        });
        return results;
    }

    private void checkBackupMetadataFile(String bsUuid, ReturnValueCompletion<Boolean> completion) {
        ReturnValueCompletion<CheckBackupMetadataFileRsp> compl = new ReturnValueCompletion<CheckBackupMetadataFileRsp>(completion) {
            @Override
            public void success(CheckBackupMetadataFileRsp rsp) {
                completion.success(rsp.exist);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        };

        if (ImageStoreBackupStorageSelector.isRemote(bsUuid)) {
            syncPost(SYNC_CHECK_BACKUP_METADATA_FILE, new AgentCommand(), bsUuid, CheckBackupMetadataFileRsp.class, compl);
        } else {
            asyncPost(CHECK_BACKUP_METADATA_FILE, new AgentCommand(), bsUuid, CheckBackupMetadataFileRsp.class, compl);
        }
    }

    private void deleteVolumeBackupsMetadata(List<String> backupUuids, String bsUuid) {
        DeleteVolumeBackupMetadataCmd cmd = new DeleteVolumeBackupMetadataCmd();
        cmd.backupUuids = backupUuids;
        ReturnValueCompletion<DeleteVolumeBackupMetadataRsp> completion = new ReturnValueCompletion<DeleteVolumeBackupMetadataRsp>(null) {
            @Override
            public void success(DeleteVolumeBackupMetadataRsp rsp) {
                logger.debug(String.format("succeed to delete backups[uuids: %s] from image store[uuid: %s]",
                        backupUuids, bsUuid));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("fail to delete backups[uuids: %s] from image store[uuid: %s]",
                        backupUuids, bsUuid));
            }
        };


        if (ImageStoreBackupStorageSelector.isRemote(bsUuid)) {
            syncPost(SYNC_DELETE_BACKUP_METADATA, cmd, bsUuid, DeleteVolumeBackupMetadataRsp.class, completion);
        } else {
            asyncPost(DELETE_BACKUP_METADATA, cmd, bsUuid, DeleteVolumeBackupMetadataRsp.class, completion);
        }
    }

    private void mergeVolumeBackupMetadataToImageStore(Collection<VolumeBackupVO> volumeBackups, String bsUuid) {
        MergeVolumeBackupMetadataCmd cmd = new MergeVolumeBackupMetadataCmd();
        List<VolumeBackupMetadata> metadatas = VolumeBackupMetadata.valueOf(volumeBackups, bsUuid);
        List<VolumeBackupMetadataExtensionPoint> exts = pluginRgty.getExtensionList(VolumeBackupMetadataExtensionPoint.class);
        for (VolumeBackupMetadata m : metadatas) {
            for (VolumeBackupMetadataExtensionPoint ext : exts) {
                try {
                    ext.collectMetadata(m.getUuid(), m.getAttachments());
                } catch (Exception e) {
                    logger.warn(String.format("extension %s failed to collect metadata for backup %s: %s",
                            ext.getClass().getName(), m.getUuid(), e.getMessage()));
                }
            }
        }
        cmd.backupMetadatas = metadatas.stream().map(JSONObjectUtil::toJsonString).collect(Collectors.toList());
        ReturnValueCompletion<MergeVolumeBackupMetadataRsp> completion = new ReturnValueCompletion<MergeVolumeBackupMetadataRsp>(null) {
            @Override
            public void success(MergeVolumeBackupMetadataRsp rsp) {
                logger.debug(String.format("succeed to merge backups[uuids: %s] to image store[uuid: %s]",
                        volumeBackups.stream().map(VolumeBackupVO::getUuid).collect(Collectors.toList()), bsUuid));
                String tag = VolumeBackupSystemTag.NO_METADATA.instantiateTag(
                        Collections.singletonMap(VolumeBackupSystemTag.NO_METADATA_BSUUID_TOKEN, bsUuid));
                volumeBackups.forEach(it -> tagMgr.deleteSystemTag(tag, it.getUuid(), VolumeBackupVO.class.getSimpleName(), false));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("fail to merge backups[uuids: %s] to image store[uuid: %s]",
                        volumeBackups.stream().map(VolumeBackupVO::getUuid).collect(Collectors.toList()), bsUuid));
            }
        };


        if (ImageStoreBackupStorageSelector.isRemote(bsUuid)) {
            syncPost(SYNC_MERGE_BACKUP_METADATA, cmd, bsUuid, MergeVolumeBackupMetadataRsp.class, completion);
        } else {
            asyncPost(MERGE_BACKUP_METADATA, cmd, bsUuid, MergeVolumeBackupMetadataRsp.class, completion);
        }
    }

    SyncBackupResult syncVolumeBackupsMetaDataFromImageStore(String bsUuid) throws IOException {
        return syncBackupsMetaDataFromImageStore(bsUuid, false);
    }

    SyncBackupResult syncVmBackupsMetaDataFromImageStore(String bsUuid) throws IOException {
        return syncBackupsMetaDataFromImageStore(bsUuid, true);
    }

    @Deferred
    private SyncBackupResult syncBackupsMetaDataFromImageStore(String bsUuid, boolean isVmBackup) throws IOException {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return new SyncBackupResult();
        }

        String tmpFile = PathUtil.createTempFile("pull-backup-", ".json");
        Defer.defer(() ->PathUtil.forceRemoveFile(tmpFile));
        ImageStoreBackupStorageVO imageStore = dbf.findByUuid(bsUuid, ImageStoreBackupStorageVO.class);
        getSsh(imageStore).scpDownload(PathUtil.join(imageStore.getUrl(), METADATA_FILE_NAME), tmpFile).runErrorByExceptionAndClose();
        return syncBackupInfo(tmpFile, bsUuid, isVmBackup);
    }

    @Deferred
    void pushVolumeBackupsMetadataToImageStore(String bsUuid) throws IOException {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        String tmpFile = PathUtil.createTempFile("push-backup-", ".json");
        Defer.defer(() ->PathUtil.forceRemoveFile(tmpFile));
        generateVolumeBackupInfo(tmpFile, bsUuid);
        ImageStoreBackupStorageVO imageStore = dbf.findByUuid(bsUuid, ImageStoreBackupStorageVO.class);
        getSsh(imageStore).scpUpload(tmpFile, PathUtil.join(imageStore.getUrl(), METADATA_FILE_NAME)).runErrorByExceptionAndClose();
    }

    private void generateVolumeBackupInfo(String path, String bsUuid) throws IOException {
        long count = Q.New(VolumeBackupStorageRefVO.class).eq(VolumeBackupStorageRefVO_.backupStorageUuid, bsUuid).count();
        if (count == 0) {
            return;
        }

        try (FileOutputStream outputStream = new FileOutputStream(new File(path))) {
            int times = (int) (count + 499) / 500;
            for (int i = 0; i < times; i++) {
                List<VolumeBackupVO> backups = SQL.New(
                        "select backup from VolumeBackupVO backup, VolumeBackupStorageRefVO ref" +
                                " where ref.backupStorageUuid = :bsUuid" +
                                " and backup.uuid = ref.volumeBackupUuid", VolumeBackupVO.class)
                        .param("bsUuid", bsUuid)
                        .limit(500).offset(i * 500)
                        .list();
                if (backups.isEmpty()) {
                    break;
                }

                List<VolumeBackupMetadata> metadatas = backups.stream().map(it -> VolumeBackupMetadata.valueOf(it, bsUuid)).collect(Collectors.toList());
                List<VolumeBackupMetadataExtensionPoint> exts = pluginRgty.getExtensionList(VolumeBackupMetadataExtensionPoint.class);
                for (VolumeBackupMetadata m : metadatas) {
                    for (VolumeBackupMetadataExtensionPoint ext : exts) {
                        try {
                            ext.collectMetadata(m.getUuid(), m.getAttachments());
                        } catch (Exception e) {
                            logger.warn(String.format("extension %s failed to collect metadata for backup %s: %s",
                            ext.getClass().getName(), m.getUuid(), e.getMessage()));
                        }
                    }
                }
                List<String> contents = metadatas.stream().map(JSONObjectUtil::toJsonString).collect(Collectors.toList());
                outputStream.write((String.join("\n", contents) + "\n").getBytes(Charset.forName("UTF-8")));
            }
        }
    }

    @Deferred
    private SyncBackupResult syncBackupInfo(String infoFile, String bsUuid, boolean isVmBackup) throws IOException {
        SyncBackupResult result = new SyncBackupResult();
        String protectTag = VolumeBackupSystemTag.NO_METADATA.instantiateTag(
                Collections.singletonMap(VolumeBackupSystemTag.NO_METADATA_BSUUID_TOKEN, bsUuid));
        List<String> creatingBackupUuids = Q.New(SystemTagVO.class).select(SystemTagVO_.resourceUuid)
                .eq(SystemTagVO_.tag, protectTag)
                .listValues();

        Map<String, VolumeBackupMetadata> toPersist = new HashMap<>();
        Q query = Q.New(VolumeBackupVO.class).select(VolumeBackupVO_.uuid);
        List<String> toRemoveUuids = isVmBackup ? query.notNull(VolumeBackupVO_.groupUuid).listValues() :
                query.isNull(VolumeBackupVO_.groupUuid) .listValues();
        toRemoveUuids.removeAll(creatingBackupUuids);

        try (InputStreamReader read = new InputStreamReader(new FileInputStream(new File(infoFile)), Charset.forName("UTF-8"))) {
            BufferedReader bufferedReader = new BufferedReader(read);
            bufferedReader.lines().forEach(line -> {
                VolumeBackupMetadata metadata = JSONObjectUtil.toObject(line, VolumeBackupMetadata.class);
                if (isVmBackup && metadata.getGroupUuid() == null || !isVmBackup && metadata.getGroupUuid() != null) {
                    return;
                }

                toPersist.put(metadata.getUuid(), metadata);
                toRemoveUuids.remove(metadata.getUuid());
                if (toPersist.size() == 200) {
                    result.newBackupCount += persistVolumeBackupIfNeed(toPersist, bsUuid);
                    toPersist.clear();
                }
            });
        }

        if (!toPersist.isEmpty()) {
            result.newBackupCount += persistVolumeBackupIfNeed(toPersist, bsUuid);
        }

        if (!toRemoveUuids.isEmpty()) {
            result.deletedBackupCount += removeVolumeBackup(toRemoveUuids, bsUuid);
        }

        return result;
    }

    private int persistVolumeBackupIfNeed(Map<String, VolumeBackupMetadata> backups, String bsUuid) {
        List<String> alreadyInDb = Q.New(VolumeBackupStorageRefVO.class).select(VolumeBackupStorageRefVO_.volumeBackupUuid)
                .eq(VolumeBackupStorageRefVO_.backupStorageUuid, bsUuid)
                .in(VolumeBackupStorageRefVO_.volumeBackupUuid, backups.keySet())
                .listValues();
        alreadyInDb.forEach(backups::remove);

        if (backups.isEmpty()) {
            return 0;
        }

        Set<String> hasNoVO = new HashSet<>(backups.keySet());
        List<String> hasVO = Q.New(VolumeBackupVO.class).select(VolumeBackupVO_.uuid)
                .in(VolumeBackupVO_.uuid, backups.keySet())
                .listValues();
        hasNoVO.removeAll(hasVO);

        List<VolumeBackupVO> toPersists = new ArrayList<>();
        List<VolumeBackupStorageRefVO> toPersistRef = new ArrayList<>();
        for (VolumeBackupMetadata metadata : backups.values()) {
            if (hasNoVO.contains(metadata.getUuid())) {
                VolumeBackupVO vo = new VolumeBackupVO();
                vo.setUuid(metadata.getUuid());
                vo.setMode(metadata.getMode());
                vo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                vo.setMetadata(metadata.getMetadata());
                vo.setCreateDate(metadata.getCreateTime());
                vo.setDescription(metadata.getDescription());
                vo.setName(metadata.getName());
                vo.setGroupUuid(metadata.getGroupUuid());
                vo.setSize(metadata.getSize());
                vo.setType(metadata.getType());
                vo.setVolumeUuid(metadata.getVolumeUuid());
                vo.setVmInstanceUuid(metadata.getVmInstanceUuid());
                vo.setState(VolumeBackupState.Enabled);
                vo.setStatus(VolumeBackupStatus.Ready);
                toPersists.add(vo);
            }

            VolumeBackupStorageRefVO ref = new VolumeBackupStorageRefVO();
            ref.setBackupStorageUuid(bsUuid);
            ref.setVolumeBackupUuid(metadata.getUuid());
            ref.setInstallPath(metadata.getInstallPath());
            ref.setStatus(VolumeBackupStatus.Ready);
            toPersistRef.add(ref);
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                databaseFacade.persistCollection(toPersists);
                databaseFacade.persistCollection(toPersistRef);
            }
        }.execute();

        List<VolumeBackupMetadataExtensionPoint> exts = pluginRgty.getExtensionList(VolumeBackupMetadataExtensionPoint.class);
        for (VolumeBackupMetadata metadata : backups.values()) {
            if (metadata.getAttachments() == null || metadata.getAttachments().isEmpty()) {
                continue;
            }

            for (VolumeBackupMetadataExtensionPoint ext : exts) {
                try {
                    ext.restoreMetadata(metadata.getUuid(), metadata.getAttachments());
                } catch (Exception e) {
                    logger.warn(String.format("extension %s failed to restore metadata for backup %s: %s",
                            ext.getClass().getName(), metadata.getUuid(), e.getMessage()));
                }
            }
        }

        return toPersistRef.size();
    }

    private int removeVolumeBackup(List<String> uuids, String bsUuid) {
        int count = uuids.size();
        for (List<String> sub : Lists.partition(uuids, 200)) {
            SQL.New(VolumeBackupStorageRefVO.class).eq(VolumeBackupStorageRefVO_.backupStorageUuid, bsUuid)
                    .in(VolumeBackupStorageRefVO_.volumeBackupUuid, sub)
                    .delete();
            List<String> hasRef = Q.New(VolumeBackupStorageRefVO.class).select(VolumeBackupStorageRefVO_.volumeBackupUuid)
                    .in(VolumeBackupStorageRefVO_.volumeBackupUuid, sub)
                    .listValues();

            List<String> toRemove = new ArrayList<>(sub);
            toRemove.removeAll(hasRef);
            dbf.removeByPrimaryKeys(toRemove, VolumeBackupVO.class);
        }
        return count;
    }

    private <T extends ImageStoreResponse> void syncPost(String path, AgentCommand cmd, String bsUuid, Class<T> rspClass, ReturnValueCompletion<T> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return "volume-backup-metadata-on-image-store-" + bsUuid;
            }

            @Override
            public void run(SyncTaskChain chain) {
                String hostName = getHostName(bsUuid);
                T rsp = restf.syncJsonPost(buildUrl(path, hostName), cmd, rspClass);
                if (rsp.isSuccess()) {
                    completion.success(rsp);
                } else {
                    completion.fail(operr("volume backup metadata operation failure, because %s",rsp.getError()));
                }
                chain.next();
            }

            @Override
            public String getName() {
                return cmd.getClass().getSimpleName() + "-" + bsUuid;
            }
        });
    }

    private <T extends ImageStoreResponse> void asyncPost(String path, AgentCommand cmd, String bsUuid, Class<T> rspClass, ReturnValueCompletion<T> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return "volume-backup-metadata-on-image-store-" + bsUuid;
            }

            @Override
            public void run(SyncTaskChain chain) {
                String hostName = getHostName(bsUuid);
                restf.asyncJsonPost(buildUrl(path, hostName), cmd, new JsonAsyncRESTCallback<T>(chain) {
                    @Override
                    public void fail(ErrorCode err) {
                        completion.fail(err);
                        chain.next();
                    }

                    @Override
                    public void success(T rsp) {
                        if (rsp.isSuccess()) {
                            completion.success(rsp);
                        } else {
                            completion.fail(operr("volume backup metadata operation failure, because %s",rsp.getError()));
                        }
                        chain.next();
                    }

                    @Override
                    public Class<T> getReturnClass() {
                        return rspClass;
                    }
                });
            }

            @Override
            public String getName() {
                return cmd.getClass().getSimpleName() + "-" + bsUuid;
            }
        });
    }

    private Ssh getSsh(ImageStoreBackupStorageVO imageStore) {
        return new Ssh().setTimeout(300)
                .setPrivateKey(asf.getPrivateKey())
                .setUsername(imageStore.getUsername())
                .setHostname(imageStore.getHostname())
                .setPort(imageStore.getSshPort());
    }

    private String getHostName(String imageStoreUuid) {
        return Q.New(ImageStoreBackupStorageVO.class).select(ImageStoreBackupStorageVO_.hostname)
                .eq(ImageStoreBackupStorageVO_.uuid, imageStoreUuid)
                .findValue();
    }

    private String buildUrl(String subPath, String hostName) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme(ImageStoreBackupStorageGlobalProperty.AGENT_URL_SCHEME);
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            ub.host("localhost");
        } else {
            ub.host(hostName);
        }

        ub.port(ImageStoreBackupStorageGlobalProperty.AGENT_PORT);
        if (!"".equals(ImageStoreBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH)) {
            ub.path(ImageStoreBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH);
        }
        ub.path(subPath);
        return ub.build().toUriString();
    }
}

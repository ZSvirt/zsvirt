package org.zstack.imagereplicator;


import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.*;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.image.ImageState;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.imagestore.SyncDatabaseBackupBetweenImageStoreMsg;
import org.zstack.header.imagestore.SyncImageBetweenImageStoreMsg;
import org.zstack.header.imagestore.SyncVolumeBackupBetweenImageStoreMsg;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.database.backup.DatabaseBackupState;
import org.zstack.header.storage.database.backup.DatabaseBackupStatus;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant;
import org.zstack.storage.backup.imagestore.ImageStoreGlobalConfig;
import org.zstack.storage.backup.imagestore.ReclaimSpaceFromImageStoreMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

/**
 * ImageReplicatorImpl implements image replication among backup storage.
 *
 * Backup Storage state and ENABLE_REPLICATION will be checked upon each
 * round of replication.
 *
 * <p>
 * The algorithm is simple (considering multiple-MN environment): we have
 * an image operation journal generator, and the here the replicator is
 * designed to replay the journal.  Visually, the process looks like:
 *
 *                      idx bs image ops
 *                      ...
 *                      4 bs1 imageX add
 * journal generator -> 5 bs1 imageY add
 *                      ...
 *
 * image replicator for BSn:
 *   1. from last replication index N from history
 *   2. find oldest journal for BSn where idx >= N
 *   3. replicate that image to BSm, where BSm and BSn are in one group.
 *   4. save history and repeat.
 * </p>
 *
 * Update:
 * Now the replicator will replicate both images and volume backups. The
 * current implementation is kind of ad-hoc: with a few if-else's.  A cleaner
 * implementation is to use plugin registry and make image/volume replication
 * as plugins.
 */
public class ImageReplicatorImpl implements ImageReplicator {
    private static final CLogger logger = Utils.getLogger(ImageReplicatorImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private CloudBus bus;

    private volatile boolean run = true;

    @Override
    public void start() {
        start_replicate();
    }

    @Override
    public void stop() {
        run = false;
    }

    @Override
    public void forceReplicate(String backupStorageUuid, Completion completion) {
        List<ImageReplicationGroupBackupStorageRefVO> refVOList =
                dbf.listAll(ImageReplicationGroupBackupStorageRefVO.class);
        if (refVOList.isEmpty()) {
            completion.success();
            return;
        }
        ReplicationGroup group = new ReplicationGroup(refVOList);
        Set<String> dstBsUuids = group.getSiblings(backupStorageUuid);
        replicateAllImages(backupStorageUuid, dstBsUuids, completion);
    }

    @AsyncThread
    private void start_replicate() {
        logger.info("starting replicator");

        while (run) {
            try {
                Long n = ImageReplicatorGlobalConfig.ScanInterval.value(Long.class);
                if (n <= 0) {
                    n = 60L;
                }
                TimeUnit.SECONDS.sleep(n);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            List<ImageReplicationGroupBackupStorageRefVO> refVOList =
                    dbf.listAll(ImageReplicationGroupBackupStorageRefVO.class);
            if (refVOList.isEmpty()) {
                logger.debug("No image replication group backup storage found, clean up ImageOpsJournalVO");
                SQL.New("delete from ImageOpsJournalVO").execute();
                continue;
            }

            replicate(new ReplicationGroup(refVOList));
        }
    }

    @ExceptionSafe
    private void replicate(ReplicationGroup rg) {
        for (String bsUuid : rg.getBackupStorageUuids()) {
                replicateImages(bsUuid, rg.getSiblings(bsUuid));
        }
    }

    /**
     * replicate images from <code>srcBsUuid</code> to <code>dstBsUuids</code>
     * @param srcBsUuid where images come from
     * @param dstBsUuids destination backup storages
     */
    private void replicateImages(String srcBsUuid, Set<String> dstBsUuids) {
        for (String dstBsUuid : dstBsUuids) {
            Optional<ImageOpsJournalVO> journalVO = pickImageToReplay(srcBsUuid, dstBsUuid);
            journalVO.ifPresent(journal -> replayJournal(journal, srcBsUuid, dstBsUuid));
        }
    }

    private void replicateAllImages(String srcBsUuid, Set<String> dstBsUuids, Completion completion) {
        ErrorCodeList errs = new ErrorCodeList();
        new While<>(dstBsUuids).each((dstBsUuid, compl) -> {
            SyncBackupStorageDataMsg msg = new SyncBackupStorageDataMsg();
            msg.setSrcBackupStorageUuid(srcBsUuid);
            msg.setDstBackupStorageUuid(dstBsUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, srcBsUuid);
            bus.send(msg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errs.getCauses().add(reply.getError());
                    }

                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errs.getCauses().isEmpty()) {
                    completion.fail(errs.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });
    }

    private Optional<ImageOpsJournalVO> pickImageToReplay(String srcBsUuid, String dstBsUuid) {
        Tuple t = Q.New(BackupStorageVO.class)
                .eq(BackupStorageVO_.uuid, dstBsUuid)
                .select(BackupStorageVO_.state, BackupStorageVO_.status)
                .findTuple();
        if (t == null) {
            return Optional.empty() ;
        }

        BackupStorageState state = t.get(0, BackupStorageState.class);
        BackupStorageStatus status = t.get(1, BackupStorageStatus.class);
        if (state != BackupStorageState.Enabled || status != BackupStorageStatus.Connected) {
            return Optional.empty();
        }

        ImageOpsJournalVO journalVO = new SQLBatchWithReturn<ImageOpsJournalVO>() {
            @Override
            protected ImageOpsJournalVO scripts() {
                Long lastIndex = q(ImageReplicationHistoryVO.class)
                        .eq(ImageReplicationHistoryVO_.backupStorageUuid, dstBsUuid)
                        .select(ImageReplicationHistoryVO_.lastIndex)
                        .orderBy(ImageReplicationHistoryVO_.lastIndex, SimpleQuery.Od.DESC)
                        .limit(1)
                        .findValue();
                long idx = lastIndex == null ? -1 : lastIndex;
                return q(ImageOpsJournalVO.class)
                        .eq(ImageOpsJournalVO_.backupStorageUuid, srcBsUuid)
                        .orderBy(ImageOpsJournalVO_.id, SimpleQuery.Od.ASC)
                        .gt(ImageOpsJournalVO_.id, idx)
                        .limit(1)
                        .find();
            }
        }.execute();

        return Optional.ofNullable(journalVO);
    }

    private void updateReplayHistory(String targetBsUuid, long index) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                if (!q(ImageReplicationHistoryVO.class)
                        .eq(ImageReplicationHistoryVO_.backupStorageUuid, targetBsUuid)
                        .isExists()) {
                    ImageReplicationHistoryVO v = new ImageReplicationHistoryVO();
                    v.setBackupStorageUuid(targetBsUuid);
                    v.setLastIndex(index);
                    persist(v);
                } else {
                    sql(ImageReplicationHistoryVO.class)
                            .eq(ImageReplicationHistoryVO_.backupStorageUuid, targetBsUuid)
                            .set(ImageReplicationHistoryVO_.lastIndex, index)
                            .update();
                }
                flush();
            }
        }.execute();

        logger.info(String.format("BS[uuid:%s] advanced reply index to %d", targetBsUuid, index));
    }

    private boolean isImageExists(String imageUuid, String sourceBsUuid) {
        Long n = SQL.New("select count(img.uuid) from ImageVO img, ImageBackupStorageRefVO refVO "
                + "where img.state = :state "
                + "and refVO.status = :status "
                + "and img.uuid = :imageUuid "
                + "and img.uuid = refVO.imageUuid "
                + "and refVO.backupStorageUuid = :bsUuid", Long.class)
                .param("imageUuid", imageUuid)
                .param("state", ImageState.Enabled)
                .param("status", ImageStatus.Ready)
                .param("bsUuid", sourceBsUuid)
                .find();
        return n != null && n != 0;
    }

    private boolean isVolumeBackupExists(String backupUuid, String sourceBsUuid) {
        Long n = SQL.New("select count(bak.uuid) from VolumeBackupVO bak, VolumeBackupStorageRefVO refVO "
                + "where bak.state = :state "
                + "and refVO.status = :status "
                + "and bak.uuid = :backupUuid "
                + "and bak.uuid = refVO.volumeBackupUuid "
                + "and refVO.backupStorageUuid = :bsUuid", Long.class)
                .param("backupUuid", backupUuid)
                .param("state", VolumeBackupState.Enabled)
                .param("status", VolumeBackupStatus.Ready)
                .param("bsUuid", sourceBsUuid)
                .find();
        return n != null && n != 0;
    }

    private boolean isDatabaseBackupExists(String backupUuid, String sourceBsUuid) {
        Long n = SQL.New("select count(bak.uuid) from DatabaseBackupVO bak, DatabaseBackupStorageRefVO refVO "
                + "where bak.state = :state "
                + "and refVO.status = :status "
                + "and bak.uuid = :backupUuid "
                + "and bak.uuid = refVO.databaseBackupUuid "
                + "and refVO.backupStorageUuid = :bsUuid", Long.class)
                .param("backupUuid", backupUuid)
                .param("state", DatabaseBackupState.Enabled)
                .param("status", DatabaseBackupStatus.Ready)
                .param("bsUuid", sourceBsUuid)
                .find();
        return n != null && n != 0;
    }

    private boolean isResourceExists(ImageOpsJournalVO journalVO, String sourceBsUuid) {
        final String resourceUuid = journalVO.getImageUuid();
        final JournalType type = journalVO.getType();

        switch (type) {
            case Image:
                return isImageExists(resourceUuid, sourceBsUuid);
            case VolumeBackup:
                return isVolumeBackupExists(resourceUuid, sourceBsUuid);
            case DatabaseBackup:
                return isDatabaseBackupExists(resourceUuid, sourceBsUuid);
            default:
                logger.warn(String.format("unexpected resource %s:%s", type.toString(), resourceUuid));
                return false;
        }
    }

    private void replayJournal(ImageOpsJournalVO journalVO, String sourceBsUuid, String targetBsUuid) {
        if (!ImageReplicatorGlobalConfig.ENABLE_REPLICATION.value(Boolean.class)) {
            return;
        }

        final String imageUuid = journalVO.getImageUuid();
        final String resourceType = journalVO.getType().toString();

        if (!destMaker.isManagedByUs(imageUuid)) {
            logger.info(String.format("%s[uuid:%s] is not managed by this node, skip.", resourceType, imageUuid));
            return;
        }

        if (!isResourceExists(journalVO, sourceBsUuid)) {
            logger.info(String.format("Resource[uuid:%s] is deleted, skip.", imageUuid));
            updateReplayHistory(targetBsUuid, journalVO.getId());
            return;
        }

        logger.info(String.format("will replicate %s[uuid:%s] from BS[uuid:%s] to target BS[uuid:%s]",
                resourceType, imageUuid, sourceBsUuid, targetBsUuid));

        switch (journalVO.getAction()) {
            case Add:    // fallthrough
            case Enable: // fallthrough
            case Retry:
                onAdd(journalVO, sourceBsUuid, targetBsUuid);
                break;
            case Delete:
                logger.info(String.format("%s[uuid:%s] is deleted from BS[uuid:%s]", resourceType, imageUuid, sourceBsUuid));
                break;
            case Expunge:
                onExpunge(journalVO, sourceBsUuid, targetBsUuid);
        }

        updateReplayHistory(targetBsUuid, journalVO.getId());
    }

    private void onExpunge(ImageOpsJournalVO journalVO, String sourceBsUuid, String targetBsUuid) {
        if (!ImageStoreGlobalConfig.CLEAN_IMAGESTORE_ON_EXPUNGE.value(Boolean.class)) {
            return;
        }

        logger.info("cleaning imagestore, uuid=" + targetBsUuid);

        ReclaimSpaceFromImageStoreMsg rmsg = new ReclaimSpaceFromImageStoreMsg();
        rmsg.setUuid(targetBsUuid);
        bus.makeTargetServiceIdByResourceUuid(rmsg, BackupStorageConstant.SERVICE_ID, rmsg.getUuid());
        bus.send(rmsg);
    }

    private void onAdd(ImageOpsJournalVO journalVO, String sourceBsUuid, String targetBsUuid) {
        FutureCompletion completion = new FutureCompletion(null);
        // TODO use factory to hide the detail about how to do replication
        do_replication(journalVO, sourceBsUuid, targetBsUuid, completion);

        while (!completion.tryWait(TimeUnit.SECONDS.toMillis(60))) {
            if (!Q.New(BackupStorageVO.class)
                    .eq(BackupStorageVO_.uuid, targetBsUuid)
                    .eq(BackupStorageVO_.status, BackupStorageStatus.Connected)
                    .eq(BackupStorageVO_.state, BackupStorageState.Enabled)
                    .isExists()) {
                completion.fail(operr("target backup storage[uuid:%s] became unavailable", targetBsUuid));
                break;
            }
        }

        if (!completion.isSuccess()) {
            final String imageUuid = journalVO.getImageUuid();
            logger.warn(String.format("replicate [%s] failed: %s", imageUuid, completion.getErrorCode().getDetails()));
            generateRetryRecord(journalVO, sourceBsUuid);
        }
    }

    private void generateRetryRecord(ImageOpsJournalVO journalVO, String sourceBsUuid) {
        ImageOpsJournalVO vo = new ImageOpsJournalVO();
        vo.setBackupStorageUuid(sourceBsUuid);
        vo.setImageUuid(journalVO.getImageUuid());
        vo.setAction(ImageAction.Retry);
        vo.setType(journalVO.getType());
        dbf.persist(vo);
    }

    private void do_replicate_image(String imageUuid, String sourceBsUuid, String targetBsUuid, Completion completion) {
        SyncImageBetweenImageStoreMsg smsg = new SyncImageBetweenImageStoreMsg();
        smsg.setImageUuid(imageUuid);
        smsg.setNewImageUuid(imageUuid);
        smsg.setSrcImageStorageUuid(sourceBsUuid);
        smsg.setDstImageStorageUuid(targetBsUuid);
        bus.makeTargetServiceIdByResourceUuid(smsg, ImageStoreBackupStorageConstant.SERVICE_ID, sourceBsUuid);
        bus.send(smsg, new CloudBusCallBack(smsg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.info("replicated image: " + imageUuid);
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private void do_replicate_volumebackup(String volumeBackupUuid, String sourceBsUuid, String targetBsUuid, Completion completion) {
        SyncVolumeBackupBetweenImageStoreMsg smsg = new SyncVolumeBackupBetweenImageStoreMsg();
        smsg.setVolumeBackupUuid(volumeBackupUuid);
        smsg.setNewVolumeBackupUuid(volumeBackupUuid);
        smsg.setSrcImageStorageUuid(sourceBsUuid);
        smsg.setDstImageStorageUuid(targetBsUuid);
        bus.makeTargetServiceIdByResourceUuid(smsg, ImageStoreBackupStorageConstant.SERVICE_ID, sourceBsUuid);
        bus.send(smsg, new CloudBusCallBack(smsg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.info("replicated volumebackup: " + volumeBackupUuid);
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private void do_replicate_databasebackup(String databaseBackupUuid, String sourceBsUuid, String targetBsUuid, Completion completion) {
        SyncDatabaseBackupBetweenImageStoreMsg smsg = new SyncDatabaseBackupBetweenImageStoreMsg();
        smsg.setDatabaseBackupUuid(databaseBackupUuid);
        smsg.setNewDatabaseBackupUuid(databaseBackupUuid);
        smsg.setSrcImageStorageUuid(sourceBsUuid);
        smsg.setDstImageStorageUuid(targetBsUuid);
        bus.makeTargetServiceIdByResourceUuid(smsg, ImageStoreBackupStorageConstant.SERVICE_ID, sourceBsUuid);
        bus.send(smsg, new CloudBusCallBack(smsg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.info("replicated database backup: " + databaseBackupUuid);
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private void do_replication(ImageOpsJournalVO journalVO, String sourceBsUuid, String targetBsUuid, Completion completion) {
        final String resourceUuid = journalVO.getImageUuid();
        if (!Q.New(ResourceVO.class).eq(ResourceVO_.uuid, resourceUuid).isExists()) {
            logger.warn(String.format("%s[uuid:%s] not found, skip replication", journalVO.getType().toString(), resourceUuid));
            completion.success();
            return;
        }

        switch (journalVO.getType()) {
            case Image:
                do_replicate_image(resourceUuid, sourceBsUuid, targetBsUuid, completion);
                break;
            case VolumeBackup:
                do_replicate_volumebackup(resourceUuid, sourceBsUuid, targetBsUuid, completion);
                break;
            case DatabaseBackup:
                do_replicate_databasebackup(resourceUuid, sourceBsUuid, targetBsUuid, completion);
                break;
        }
    }
}

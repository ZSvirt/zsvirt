package org.zstack.imagereplicator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MarshalReplyMessageExtensionPoint;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.*;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.AfterUnpackBackupStorageExtensionPoint;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.VolumeBackupInventory;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.storage.database.backup.CreateDatabaseBackupReply;
import org.zstack.header.storage.database.backup.DeleteDatabaseBackupExtensionPoint;
import org.zstack.header.storage.primary.CommitVolumeAsImageReply;
import org.zstack.header.storage.volume.backup.CreateVolumeBackupExtensionPoint;
import org.zstack.header.storage.volume.backup.DeleteVolumeBackupExtensionPoint;
import org.zstack.storage.backup.imagestore.APIRecoveryImageFromImageStoreBackupStorageEvent;
import org.zstack.storage.backup.imagestore.APISyncImageFromImageStoreBackupStorageEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ReplicationGroupManagerImpl extends AbstractService implements
        ReplicationGroupManager,
        AddImageExtensionPoint, CreateTemplateExtensionPoint,
        ExpungeImageExtensionPoint, DeleteVolumeBackupExtensionPoint, DeleteDatabaseBackupExtensionPoint,
        CreateVolumeBackupExtensionPoint,
        MarshalReplyMessageExtensionPoint, AfterUnpackBackupStorageExtensionPoint,
        ManagementNodeReadyExtensionPoint {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ImageJournalGenerator imageJournalGenerator;
    @Autowired
    private VolumeBackupJournalGenerator volumeBackupJournalGenerator;
    @Autowired
    private DatabaseBackupJournalGenerator databaseBackupJournalGenerator;
    @Autowired
    private ImageReplicator replicator;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APICreateImageReplicationGroupMsg) {
            handle((APICreateImageReplicationGroupMsg) msg);
        } else if (msg instanceof APIDeleteImageReplicationGroupMsg) {
            handle((APIDeleteImageReplicationGroupMsg) msg);
        } else if (msg instanceof APIAddBackupStoragesToReplicationGroupMsg) {
            handle((APIAddBackupStoragesToReplicationGroupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICreateImageReplicationGroupMsg msg) {
        APICreateImageReplicationGroupEvent evt = new APICreateImageReplicationGroupEvent(msg.getId());
        ImageReplicationGroupVO vo = new ImageReplicationGroupVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }

        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setState(ReplicationGroupState.Enabled);
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        dbf.persistAndRefresh(vo);

        evt.setInventory(ImageReplicationGroupInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void doDeleteImageReplicationGroup(String rgUuid, NoErrorCompletion completion) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(ImageReplicationGroupBackupStorageRefVO.class)
                        .eq(ImageReplicationGroupBackupStorageRefVO_.replicationGroupUuid, rgUuid)
                        .delete();
                sql(ImageReplicationGroupVO.class)
                        .eq(ImageReplicationGroupVO_.uuid, rgUuid)
                        .hardDelete();
            }
        }.execute();

        completion.done();
    }

    private void handle(APIDeleteImageReplicationGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "replication-group";
            }

            @Override
            public void run(SyncTaskChain chain) {
                doDeleteImageReplicationGroup(msg.getUuid(), new NoErrorCompletion(msg) {
                    @Override
                    public void done() {
                        bus.publish(new APIDeleteImageReplicationGroupEvent(msg.getId()));
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-replication-group-%s", msg.getUuid());
            }
        });
    }

    private void addBackupStorageToRepGroup(String repGroupUuid, List<String> bsUuids,
                                            ReturnValueCompletion<List<ImageReplicationGroupBackupStorageRefInventory>> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return "replication-group";
            }

            @Override
            public void run(SyncTaskChain chain) {
                Collection<ImageReplicationGroupBackupStorageRefVO> refs = new SQLBatchWithReturn<Collection<ImageReplicationGroupBackupStorageRefVO>>() {
                    @Override
                    protected Collection<ImageReplicationGroupBackupStorageRefVO> scripts() {
                        for (String bsUuid : bsUuids) {
                            ImageReplicationGroupBackupStorageRefVO refVO = new ImageReplicationGroupBackupStorageRefVO();
                            refVO.setBackupStorageUuid(bsUuid);
                            refVO.setReplicationGroupUuid(repGroupUuid);
                            persist(refVO);
                        }

                        return q(ImageReplicationGroupBackupStorageRefVO.class)
                                .eq(ImageReplicationGroupBackupStorageRefVO_.replicationGroupUuid, repGroupUuid)
                                .in(ImageReplicationGroupBackupStorageRefVO_.backupStorageUuid, bsUuids)
                                .list();
                    }
                }.execute();

                bsUuids.forEach(bsUuid -> imageJournalGenerator.generateInitialRecords(bsUuid));
                completion.success(ImageReplicationGroupBackupStorageRefInventory.valueOf(refs));
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("add-bs-to-replication-group-%s", repGroupUuid);
            }
        });
    }

    private void handle(APIAddBackupStoragesToReplicationGroupMsg msg) {
        APIAddBackupStoragesToReplicationGroupEvent evt = new APIAddBackupStoragesToReplicationGroupEvent(msg.getId());
        addBackupStorageToRepGroup(msg.getReplicationGroupUuid(), msg.getBackupStorageUuids(),
                new ReturnValueCompletion<List<ImageReplicationGroupBackupStorageRefInventory>>(msg) {
                    @Override
                    public void success(List<ImageReplicationGroupBackupStorageRefInventory> refs) {
                        evt.setInventories(refs);
                        bus.publish(evt);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                    }
                }
        );
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ReplicationGroupConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        replicator.stop();
        return true;
    }

    @Override
    public void managementNodeReady() {
        startReplicator();
    }

    @Override
    public List<Class> getReplyMessageClassForMarshalExtensionPoint() {
        // We don't handle DeleteMsg - the replicator should check:
        //  - image status (ignore disabled/deleted)
        //  - BS status (pause replication upon disabled)
        return Arrays.asList(
                CommitVolumeAsImageReply.class,
                CreateDatabaseBackupReply.class,

                APIRecoverImageEvent.class,
                APIUpdateImageEvent.class,
                APISyncImageFromImageStoreBackupStorageEvent.class,
                APIRecoveryImageFromImageStoreBackupStorageEvent.class
        );
    }

    @Override
    public void afterCreateTemplate(ImageInventory inv) {
        imageJournalGenerator.onAddImage(inv);
    }

    @Override
    public void validateAddImage(List<String> bsUuids) {
    }

    @Override
    public void preAddImage(ImageInventory img) {
    }

    @Override
    public void beforeAddImage(ImageInventory img) {
    }

    @Override
    public void afterAddImage(ImageInventory img) {
        imageJournalGenerator.onAddImage(img);
    }

    @Override
    public void failedToAddImage(ImageInventory img, ErrorCode err) {
    }

    @Override
    public void preExpungeImage(ImageInventory img) {
    }

    @Override
    public void beforeExpungeImage(ImageInventory img) {
    }

    @Override
    public void afterExpungeImage(ImageInventory img, String imageBackupStorageUuid) {
        imageJournalGenerator.onExpungeImage(img.getUuid(), imageBackupStorageUuid);
    }

    @Override
    public void failedToExpungeImage(ImageInventory img, ErrorCode err) {
    }

    @Override
    public void beforeCreateVolumeBackup(VolumeBackupVO vo, String bsUuid) {
    }

    @Override
    public void failedToCreateVolumeBackup(VolumeBackupVO vo, String bsUuid) {
    }

    @Override
    public void afterCreateVolumeBackup(VolumeBackupVO vo, String bsUuid) {
        volumeBackupJournalGenerator.onAddVolumeBackup(VolumeBackupInventory.valueOf(vo));
    }

    @Override
    public void afterDeleteVolumeBackup(String backupUuid, List<String> bsUuids) {
        for (String bsUuid: bsUuids) {
            volumeBackupJournalGenerator.onExpungeVolumeBackup(backupUuid, bsUuid);
        }
    }

    @Override
    public void afterDeleteDatabaseBackup(String backupUuid, List<String> bsUuids) {
        for (String bsUuid: bsUuids) {
            databaseBackupJournalGenerator.onExpungeDatabaseBackup(backupUuid, bsUuid);
        }
    }

    @Override
    public void marshalReplyMessageBeforeSending(Message replyOrEvent, NeedReplyMessage msg) {
        if (replyOrEvent instanceof MessageReply) {
            MessageReply reply = (MessageReply) replyOrEvent;
            if (!reply.isSuccess()) {
                return;
            }

            if (replyOrEvent instanceof CommitVolumeAsImageReply) {
                imageJournalGenerator.onAddImage(((CommitVolumeAsImageReply) replyOrEvent).getInventory());
            } else if (replyOrEvent instanceof CreateDatabaseBackupReply) {
                databaseBackupJournalGenerator.onAddDatabaseBackup(((CreateDatabaseBackupReply) reply).getInventory());
            }

            return;
        }

        if (!(replyOrEvent instanceof APIEvent)) {
            return;
        }

        if (!((APIEvent) replyOrEvent).isSuccess()) {
            return;
        }

        if (replyOrEvent instanceof APIRecoverImageEvent) {
            imageJournalGenerator.onAddImage(((APIRecoverImageEvent) replyOrEvent).getInventory());
        } else if (replyOrEvent instanceof APIUpdateImageEvent) {
            imageJournalGenerator.onUpdateImage(((APIUpdateImageEvent) replyOrEvent).getInventory());
        } else if (replyOrEvent instanceof APISyncImageFromImageStoreBackupStorageEvent) {
            imageJournalGenerator.onAddImage(((APISyncImageFromImageStoreBackupStorageEvent) replyOrEvent).getInventory());
        } else if (replyOrEvent instanceof APIRecoveryImageFromImageStoreBackupStorageEvent) {
            imageJournalGenerator.onAddImage(((APIRecoveryImageFromImageStoreBackupStorageEvent) replyOrEvent).getInventory());
        }
    }

    private void startReplicator() {
        replicator.start();
    }

    @Override
    public void afterUnpackBackupStorage(BackupStorageInventory inventory, Completion completion) {
        replicator.forceReplicate(inventory.getUuid(), completion);
    }
}

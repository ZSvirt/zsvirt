package org.zstack.storage.backup.imagestore;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.asyncbatch.LoopAsyncBatch;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.image.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.CreateTemplateFromRootVolumeSnapShotVmMsg;
import org.zstack.header.vm.CreateTemplateFromRootVolumeVmMsg;
import org.zstack.header.vm.CreateTemplateFromRootVolumeVmReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.image.ImageExtensionPointEmitter;
import org.zstack.image.ImageManager;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionUtils.transform;

/**
 * Created by david on 8/2/16.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CommitVolumeAsImageFlowChain {
    private static final CLogger logger = Utils.getLogger(CommitVolumeAsImageFlowChain.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ImageExtensionPointEmitter imageExtEmitter;
    @Autowired
    private ImageManager imageMgr;

    // It is of CommitVolumeAsImageMsg.
    private final CommitVolumeAsImageMsg msg;
    private final String messageId;
    private final VolumeInventory volume;
    private String snapshotUuid;
    private boolean fastCreate;

    public CommitVolumeAsImageFlowChain() {
        this.msg = null;
        this.messageId = null;
        this.volume = null;
        this.snapshotUuid = null;
    }

    private CommitVolumeAsImageFlowChain(final CommitVolumeAsImageMsg msg) {
        this.msg = msg;
        this.messageId = msg.getId();
        this.volume = VolumeInventory.valueOf(dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class));
        if (msg instanceof CommitVolumeSnapshotAsImageMsg) {
            this.snapshotUuid = ((CommitVolumeSnapshotAsImageMsg) msg).getVolumeSnapshotUuid();
        }
        this.fastCreate = msg.hasSystemTag(VolumeSystemTags.FAST_CREATE.getTagFormat());
    }

    public BackupStorageInventory selectBackupStorage(VolumeInventory vol, long size) {
        return doSelectBS(vol, size, null);
    }

    public BackupStorageInventory selectBackupStorage(VolumeInventory vol, long size, List<String> requiredTypes) {
        return doSelectBS(vol, size, requiredTypes);
    }

    private BackupStorageInventory doSelectBS(VolumeInventory vol, long size, List<String> requiredTypes) {
        for (CommitImageBackupStorageSelector s : pluginRgty.getExtensionList(CommitImageBackupStorageSelector.class)) {
            BackupStorageInventory bs = s.selectWithVolume(vol, size);
            if (bs == null || (requiredTypes != null && !requiredTypes.contains(bs.getType()))) {
                continue;
            }

            if (bs.getType().equals(ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE)) {
                return bs;
            }

            logger.info(String.format("Root image for volume [%s] is not image store", vol.getUuid()));
        }

        return null;
    }

    private String getMsgId() {
        return messageId;
    }

    public static CommitVolumeAsImageFlowChain getFlow(final CommitVolumeAsImageMsg msg) {
        return new CommitVolumeAsImageFlowChain(msg);
    }

    private void doCommit(final String backupStorageUuid, final ImageVO imageVO, Completion completion) {
        CommitVolumeAsImageOnPrimaryStorageMsg abmsg = new CommitVolumeAsImageOnPrimaryStorageMsg();
        if (this.snapshotUuid != null) {
            abmsg = new CommitVolumeSnapshotAsImageOnPrimaryStorageMsg();
            ((CommitVolumeSnapshotAsImageOnPrimaryStorageMsg) abmsg).setSnapshotUuid(this.snapshotUuid);
        }

        PrimaryStorageMessage m = (PrimaryStorageMessage) msg;
        abmsg.setPrimaryStorageUuid(m.getPrimaryStorageUuid());
        abmsg.setVolumeUuid(msg.getVolumeUuid());
        abmsg.setBackupStorageUuid(backupStorageUuid);
        abmsg.setImageUuid(imageVO.getUuid());
        ImageBackupStorageRefVO ref = persistRefVOByBsInventory(backupStorageUuid,imageVO.getUuid());
        // send the message to primary storage backend for processing
        bus.makeTargetServiceIdByResourceUuid(abmsg, PrimaryStorageConstant.SERVICE_ID, m.getPrimaryStorageUuid());
        bus.send(abmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (dbf.reload(imageVO) == null) {
                    SQL.New("delete from ImageBackupStorageRefVO where imageUuid = :uuid")
                            .param("uuid", imageVO.getUuid())
                            .execute();
                    completion.fail(operr("image [uuid:%s] has been deleted", imageVO.getUuid()));
                    return;
                }

                if (!reply.isSuccess()) {
                    dbf.remove(ref);
                    completion.fail(reply.getError());
                    return;
                }

                CommitVolumeAsImageOnPrimaryStorageReply r = reply.castReply();

                ref.setInstallPath(r.getBackupStorageInstallPath());
                ref.setStatus(ImageStatus.Ready);
                dbf.update(ref);

                imageVO.setActualSize(r.getActualSize());
                imageVO.setSize(r.getSize());
                if (r.getFormat() != null) {
                    imageVO.setFormat(r.getFormat());
                }
                dbf.update(imageVO);
                completion.success();
            }
        });
    }

    private void doTemporaryCommit(ImageVO imageVO, VolumeSnapshotInventory snapshot, Completion completion) {
        CreateImageCacheFromVolumeSnapshotMsg cmsg = new CreateImageCacheFromVolumeSnapshotMsg();
        cmsg.setSnapshotUuid(snapshot.getUuid());
        cmsg.setImageUuid(imageVO.getUuid());
        cmsg.setVolumeUuid(volume.getUuid());
        cmsg.setTreeUuid(snapshot.getTreeUuid());
        cmsg.setSystemTags(msg.getSystemTags());
        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeSnapshotConstant.SERVICE_ID, volume.getUuid());
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    completion.fail(r.getError());
                    return;
                }

                CreateImageCacheFromVolumeSnapshotReply cr = r.castReply();
                imageVO.setActualSize(cr.getActualSize());
                imageVO.setSize(volume.getSize());
                if (cr.getImageUrl() != null) {
                    imageVO.setUrl(cr.getImageUrl());
                }
                dbf.update(imageVO);
                completion.success();
            }
        });
    }

    private void doLegacyCommit(final String backupStorageUuid, final ImageVO imageVO, Completion completion) {
        if (ImageConstant.ImageMediaType.RootVolumeTemplate.toString().equals(msg.getMediaType())) {
            doRootVolumeLegacyCommit(backupStorageUuid, imageVO, completion);
        } else {
            doDataVolumeLegacyCommit(backupStorageUuid, imageVO, completion);
        }
    }

    private void doDataVolumeLegacyCommit(final String backupStorageUuid, final ImageVO imageVO, Completion completion) {
        CreateDataVolumeTemplateFromDataVolumeMsg cmsg = new CreateDataVolumeTemplateFromDataVolumeMsg();
        if (this.snapshotUuid != null) {
            cmsg = new CreateDataVolumeTemplateFromDataVolumeSnapshotMsg();
            ((CreateDataVolumeTemplateFromDataVolumeSnapshotMsg) cmsg).setSnapshotUuid(this.snapshotUuid);
        }

        ImageInventory img = ImageInventory.valueOf(imageVO);
        cmsg.setBackupStorageUuid(backupStorageUuid);
        cmsg.setVolumeUuid(volume.getUuid());
        cmsg.setImageUuid(img.getUuid());

        ImageBackupStorageRefVO ref = persistRefVOByBsInventory(backupStorageUuid, imageVO.getUuid());

        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeConstant.SERVICE_ID, volume.getUuid());
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    dbf.remove(ref);
                    completion.fail(reply.getError());
                    return;
                }

                CreateDataVolumeTemplateFromDataVolumeReply r = reply.castReply();
                ref.setInstallPath(r.getInstallPath());
                ref.setStatus(ImageStatus.Ready);
                dbf.update(ref);

                imageVO.setActualSize(volume.getActualSize());
                imageVO.setSize(volume.getSize());
                if (r.getFormat() != null) {
                    imageVO.setFormat(r.getFormat());
                }
                dbf.update(imageVO);
                completion.success();
            }
        });
    }

    private void doRootVolumeLegacyCommit(final String backupStorageUuid, final ImageVO imageVO, Completion completion) {
        CreateTemplateFromRootVolumeVmMsg cmsg = new CreateTemplateFromRootVolumeVmMsg();
        if (this.snapshotUuid != null) {
            cmsg = new CreateTemplateFromRootVolumeSnapShotVmMsg();
            ((CreateTemplateFromRootVolumeSnapShotVmMsg) cmsg).setSnapshotUuid(this.snapshotUuid);
        }
        ImageInventory img = ImageInventory.valueOf(imageVO);
        cmsg.setBackupStorageUuid(backupStorageUuid);
        cmsg.setRootVolumeInventory(volume);
        cmsg.setImageInventory(img);

        ImageBackupStorageRefVO ref = persistRefVOByBsInventory(backupStorageUuid, imageVO.getUuid());

        bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, volume.getVmInstanceUuid());
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    dbf.remove(ref);
                    completion.fail(reply.getError());
                    return;
                }

                CreateTemplateFromRootVolumeVmReply r = (CreateTemplateFromRootVolumeVmReply) reply;
                ref.setInstallPath(r.getInstallPath());
                ref.setStatus(ImageStatus.Ready);
                dbf.update(ref);

                imageVO.setActualSize(volume.getActualSize());
                imageVO.setSize(volume.getSize());
                if (r.getFormat() != null) {
                    imageVO.setFormat(r.getFormat());
                }
                dbf.update(imageVO);
                completion.success();
            }
        });
    }

    public void run() {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-template-from-%s-volume-%s", volume.getType().toLowerCase(), volume.getUuid()));
        chain.enableProgressReport();
        chain.then(new ShareFlow() {
            ImageVO imageVO;
            Long imageEstimateSize;
            List<BackupStorageInventory> backupStorages = new ArrayList<>();

            @Override
            public void setup() {

                flow(new NoRollbackFlow() {
                    String __name__ = "get-volume-actual-size";

                    @Override
                    public boolean skip(Map data) {
                        return fastCreate;
                    }

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (msg.getVolumeActualSize() > 0) {
                            imageEstimateSize = msg.getVolumeActualSize();
                            trigger.next();
                            return;
                        }

                        EstimateVolumeTemplateSizeMsg msg = new EstimateVolumeTemplateSizeMsg();
                        msg.setVolumeUuid(volume.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                EstimateVolumeTemplateSizeReply sr = reply.castReply();
                                volume.setActualSize(sr.getActualSize());
                                volume.setSize(sr.getSize());
                                imageEstimateSize = sr.getActualSize();
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = String.format("find-storage-to-commit-image-for-volume-%s", msg.getVolumeUuid());

                    @Override
                    public boolean skip(Map data) {
                        return fastCreate;
                    }

                    void allocateSpecifiedBackupStorage(FlowTrigger trigger) {
                        List<ErrorCode> errorCodes = new ArrayList<>();
                        new While<>(msg.getBackupStorageUuids()).all((backupStorageUuid, completion) -> {

                            AllocateBackupStorageMsg abmsg = new AllocateBackupStorageMsg();
                            abmsg.setSize(imageEstimateSize);
                            abmsg.setBackupStorageUuid(backupStorageUuid);
                            bus.makeLocalServiceId(abmsg, BackupStorageConstant.SERVICE_ID);

                            bus.send(abmsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        backupStorages.add(((AllocateBackupStorageReply) reply).getInventory());
                                    } else {
                                        errorCodes.add(reply.getError());
                                    }
                                    completion.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodes.isEmpty()) {
                                    trigger.next();
                                    return;
                                }

                                ErrorCode ec = multiErr(errorCodes, "unable to allocate backup storage specified by uuids: %s, becasue: %s",
                                        String.join(",", msg.getBackupStorageUuids()), errorCodes.get(0).getDetails());
                                trigger.fail(ec);
                            }
                        });
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        // If this message is called indirectly by APICloneVmInstanceMsg, we do not have
                        // backup storage uuid.  If APICreateRootVolumeTemplateFromRootVolumeMsg is called
                        // directly, its backup storage uuid field will be validated by the validator and
                        // can't be null (and in this case it will fail early before reaching here).
                        if (msg.getBackupStorageUuids() != null && !msg.getBackupStorageUuids().isEmpty()) {
                            allocateSpecifiedBackupStorage(trigger);
                            return;
                        }

                        final VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class));
                        final BackupStorageInventory bsinv = selectBackupStorage(vol, imageEstimateSize);

                        if (bsinv == null) {
                            ErrorCode ec = err(
                                    SysErrors.RESOURCE_NOT_FOUND,
                                    "No backup storage to commit volume [uuid: %s]", msg.getVolumeUuid()
                            );
                            trigger.fail(ec);
                            return;
                        }

                        AllocateBackupStorageMsg abmsg = new AllocateBackupStorageMsg();
                        abmsg.setSize(imageEstimateSize);
                        abmsg.setBackupStorageUuid(bsinv.getUuid());
                        bus.makeLocalServiceId(abmsg, BackupStorageConstant.SERVICE_ID);

                        bus.send(abmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                backupStorages.add(((AllocateBackupStorageReply) reply).getInventory());
                                msg.setBackupStorageUuids(Collections.singletonList(bsinv.getUuid()));
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (backupStorages.isEmpty()) {
                            trigger.rollback();
                            return;
                        }

                        List<ReturnBackupStorageMsg> rmsgs = transform(backupStorages, arg -> {
                            ReturnBackupStorageMsg rmsg = new ReturnBackupStorageMsg();
                            rmsg.setBackupStorageUuid(arg.getUuid());
                            rmsg.setSize(imageEstimateSize);
                            bus.makeLocalServiceId(rmsg, BackupStorageConstant.SERVICE_ID);
                            return rmsg;
                        });

                        new While<>(rmsgs).all((msg, completion) -> {
                            bus.send(msg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    logger.warn(String.format("failed to return %s bytes to backup storage[uuid:%s]", imageEstimateSize, msg.getBackupStorageUuid()));
                                    completion.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "create-image-in-database";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        imageVO = imageMgr.createImageInDb(msg, imvo -> {
                            imvo.setFormat(volume.getFormat());
                            imvo.setUrl(String.format("volume://%s", msg.getVolumeUuid()));
                        });

                        msg.getBackupStorageUuids().forEach(bsUuid -> imageExtEmitter.beforeCreateImage(
                                ImageInventory.valueOf(imageVO), bsUuid, msg.getPrimaryStorageUuid()
                        ));

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (imageVO != null) {
                            dbf.remove(imageVO);
                        }
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "commit-volume-as-image-on-primary-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (fastCreate) {
                            createTemporary(trigger);
                        } else {
                            commitToBs(trigger);
                        }
                    }

                    private void createTemporary(FlowTrigger trigger) {
                        Completion compl = new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        };

                        if (snapshotUuid != null) {
                            VolumeSnapshotInventory snap = dbf.findByUuid(snapshotUuid, VolumeSnapshotVO.class).toInventory();
                            doTemporaryCommit(imageVO, snap, compl);
                            return;
                        }

                        VolumeCreateSnapshotMsg smsg = new VolumeCreateSnapshotMsg();
                        smsg.setAccountUuid(msg.getSession().getAccountUuid());
                        smsg.setVolumeUuid(volume.getUuid());
                        smsg.setName("for-temporary-image-" + imageVO.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(smsg, VolumeConstant.SERVICE_ID, volume.getUuid());
                        bus.send(smsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                VolumeCreateSnapshotReply cr = reply.castReply();
                                doTemporaryCommit(imageVO, cr.getInventory(), compl);
                            }
                        });
                    }

                    private void commitToBs(FlowTrigger trigger) {
                        List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();
                        //TODO: how to define async task progress ?
                        new LoopAsyncBatch<String>(trigger) {
                            @Override
                            protected Collection<String> collect() {
                                return msg.getBackupStorageUuids();
                            }

                            @Override
                            protected AsyncBatchRunner forEach(String backupStorageUuid) {
                                return new AsyncBatchRunner() {
                                    @Override
                                    public void run(NoErrorCompletion completion) {
                                        BackupStorageVO bsvo = dbf.findByUuid(backupStorageUuid, BackupStorageVO.class);
                                        Completion comp = new Completion(completion) {
                                            @Override
                                            public void success() {
                                                imageVO = dbf.reload(imageVO);
                                                completion.done();
                                            }

                                            @Override
                                            public void fail(ErrorCode errorCode) {
                                                synchronized (errorCodes) {
                                                    errorCodes.add(errorCode);
                                                    completion.done();
                                                }
                                            }
                                        };

                                        if (bsvo.getType().equals(ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE)) {
                                            doCommit(backupStorageUuid, imageVO, comp);
                                        } else {
                                            doLegacyCommit(backupStorageUuid, imageVO, comp);
                                        }
                                    }
                                };
                            }

                            @Override
                            protected void done() {
                                if (msg.getBackupStorageUuids().size() == errorCodes.size()) {
                                    ErrorCode ec = multiErr(errorCodes, "unable to commit backup storage specified by uuids: %s",
                                            msg.getBackupStorageUuids());
                                    trigger.fail(ec);
                                } else {
                                    trigger.next();
                                }
                            }
                        }.start();
                    }
                });


                flow(new NoRollbackFlow() {
                    String __name__ = String.format("sync-image-size");

                    @Override
                    public boolean skip(Map data) {
                        return fastCreate;
                    }

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {

                        new While<>(msg.getBackupStorageUuids()).all((arg, completion) -> {
                            SyncImageSizeMsg smsg = new SyncImageSizeMsg();
                            smsg.setBackupStorageUuid(arg);
                            smsg.setImageUuid(imageVO.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(smsg, ImageConstant.SERVICE_ID, imageVO.getUuid());
                            bus.send(smsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    completion.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "copy-system-tag-to-image";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SyncSystemTagFromVolumeMsg smsg = new SyncSystemTagFromVolumeMsg();
                        smsg.setImageUuid(imageVO.getUuid());
                        smsg.setVolumeUuid(msg.getVolumeUuid());
                        bus.makeLocalServiceId(smsg, ImageConstant.SERVICE_ID);
                        bus.send(smsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("sync image[uuid:%s]system tag fail", msg.getVolumeUuid()));
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        final CommitVolumeAsImageReply reply = new CommitVolumeAsImageReply();
                        reply.setId(getMsgId());
                        imageVO = dbf.reload(imageVO);
                        imageVO.setStatus(ImageStatus.Ready);
                        imageVO = dbf.updateAndRefresh(imageVO);
                        ImageInventory iinv = ImageInventory.valueOf(imageVO);

                        imageExtEmitter.afterCreateImage(iinv);

                        reply.setInventory(iinv);
                        logger.debug(String.format("successfully create template[uuid:%s] from volume[uuid:%s, type:%s]",
                                iinv.getUuid(), volume.getUuid(), volume.getType()));
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        final CommitVolumeAsImageReply reply = new CommitVolumeAsImageReply();
                        reply.setId(getMsgId());
                        reply.setError(errCode);
                        logger.warn(String.format("failed to create template from volume[uuid:%s, type:%s], because %s",
                                volume.getUuid(), volume.getType(), errCode));
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private ImageBackupStorageRefVO persistRefVOByBsInventory(String bsUuid, String imageUuid) {
        ImageBackupStorageRefVO ref = new ImageBackupStorageRefVO();
        ref.setBackupStorageUuid(bsUuid);
        ref.setStatus(ImageStatus.Creating);
        ref.setImageUuid(imageUuid);
        ref.setInstallPath("");
        return dbf.persistAndRefresh(ref);
    }
}

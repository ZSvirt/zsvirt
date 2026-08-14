package org.zstack.storage.primary.imagestore.ceph;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.SyncSystemTagFromVolumeMsg;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.CommitVolumeSnapshotAsImageMsg;
import org.zstack.header.storage.snapshot.CreateImageCacheFromVolumeSnapshotMsg;
import org.zstack.header.storage.snapshot.CreateImageCacheFromVolumeSnapshotReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.CreateTemplateFromRootVolumeSnapShotVmMsg;
import org.zstack.header.vm.CreateTemplateFromRootVolumeVmMsg;
import org.zstack.header.vm.CreateTemplateFromRootVolumeVmReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.volume.*;
import org.zstack.header.volume.block.BlockVolumeVO;
import org.zstack.header.volume.block.BlockVolumeVO_;
import org.zstack.identity.AccountManager;
import org.zstack.image.ImageExtensionPointEmitter;
import org.zstack.image.ImageManager;
import org.zstack.storage.backup.imagestore.CleanImageMetaOnPrimaryStorageMsg;
import org.zstack.storage.backup.imagestore.CleanImageMetaOnPrimaryStorageReply;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.CephSystemTags;
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.storage.volume.block.BlockPrimaryStorageBackend;
import org.zstack.storage.volume.block.BlockPrimaryStorageFactory;
import org.zstack.header.volume.block.GetAccessPathMsg;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * Created by david on 8/9/16.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CephPrimaryStorageImageStoreBackend extends CephPrimaryStorageBase {
    private static final CLogger logger = Utils.getLogger(CephPrimaryStorageImageStoreBackend.class);

    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ImageExtensionPointEmitter imageExtEmitter;
    @Autowired
    private ImageManager imageMgr;
    @Autowired
    private CephBlockPrimaryStorageManager blockMgr;

    public CephPrimaryStorageImageStoreBackend() {
    }

    public static class CommitImageCmd extends AgentCommand {
        private String snapshotPath;
        private String dstPath;
        private boolean ignoreError;

        public boolean isIgnoreError() {
            return ignoreError;
        }

        public void setIgnoreError(boolean ignoreError) {
            this.ignoreError = ignoreError;
        }

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }

        public String getDstPath() {
            return dstPath;
        }

        public void setDstPath(String dstPath) {
            this.dstPath = dstPath;
        }
    }

    public static class CommitImageRsp extends AgentResponse {
        private Long size;

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

    }

    public static class ResizeVolumeCmd extends AgentCommand {
        private String installPath;
        private long size;
        private boolean force;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public boolean isForce() {
            return force;
        }

        public void setForce(boolean force) {
            this.force = force;
        }
    }

    public static class ResizeVolumeRsp extends AgentResponse {
        private long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static final String RESIZE_VOLUME_PATH = "/ceph/primarystorage/volume/resize";

    class CephCommitVolumeAsImageFlowChain {

        // It is of or CommitVolumeAsImageMsg.
        private final CommitVolumeAsImageMsg msg;
        private final String messageId;
        private final VolumeInventory volume;
        private String snapshotUuid;
        private final boolean fastCreate;

        public CephCommitVolumeAsImageFlowChain() {
            this.msg = null;
            this.messageId = null;
            this.volume = null;
            this.snapshotUuid = null;
            this.fastCreate = false;
        }

        CephCommitVolumeAsImageFlowChain(final CommitVolumeAsImageMsg msg) {
            this.msg = msg;
            this.messageId = msg.getId();
            this.volume = VolumeInventory.valueOf(dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class));
            if (msg instanceof CommitVolumeSnapshotAsImageMsg) {
                this.snapshotUuid = ((CommitVolumeSnapshotAsImageMsg) msg).getVolumeSnapshotUuid();
            }

            fastCreate = msg.hasSystemTag(VolumeSystemTags.FAST_CREATE.getTagFormat());
        }

        private BackupStorageInventory selectBackupStorage(VolumeInventory vol, long requiredSize) {
            return selectBackupStorage(vol, requiredSize, null);
        }

        private BackupStorageInventory selectBackupStorage(VolumeInventory vol, long requiredSize, List<String> requiredTypes) {
            List<BackupStorageInventory> bss = new ArrayList<>();
            for (CommitImageBackupStorageSelector s : pluginRgty.getExtensionList(CommitImageBackupStorageSelector.class)) {
                BackupStorageInventory bs = s.selectWithVolume(vol, requiredSize);
                if (bs == null || (requiredTypes != null && !requiredTypes.contains(bs.getType()))) {
                    continue;
                }

                // support ceph and imagestore both
                if (bs.getType().equals(CephConstants.CEPH_BACKUP_STORAGE_TYPE)) {
                    return bs;
                } else if (bs.getType().equals(ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE)) {
                    bss.add(bs);
                }
            }
            if (bss.size() == 0) {
                return null;
            } else {
                return bss.get(0);
            }
        }

        private String getMsgId() {
            return messageId;
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

            cmsg.setBackupStorageUuid(backupStorageUuid);
            cmsg.setVolumeUuid(volume.getUuid());
            cmsg.setImageUuid(imageVO.getUuid());
            cmsg.setQueuedInVolume(!PrimaryStorageGlobalConfig.UNDO_TEMP_SNAPSHOT.value(Boolean.class));
            ImageBackupStorageRefVO ref = persistRefVOByBsInventory(backupStorageUuid, imageVO.getUuid());
            bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeConstant.SERVICE_ID, volume.getUuid());
            bus.send(cmsg, new CloudBusCallBack(completion) {
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

                    CreateDataVolumeTemplateFromDataVolumeReply r = reply.castReply();
                    ref.setStatus(ImageStatus.Ready);
                    ref.setInstallPath(r.getInstallPath());
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

                    CreateTemplateFromRootVolumeVmReply r = (CreateTemplateFromRootVolumeVmReply) reply;
                    ref.setStatus(ImageStatus.Ready);
                    ref.setInstallPath(r.getInstallPath());
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
                String targetBackupStorageUuid;

                @Override
                public void setup() {
                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("find-storage-to-commit-image-for-volume-%s", msg.getVolumeUuid());

                        @Override
                        public boolean skip(Map data) {
                            return fastCreate;
                        }

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            if (!CollectionUtils.isEmpty(msg.getBackupStorageUuids())) {
                                targetBackupStorageUuid = msg.getBackupStorageUuids().get(0);
                                trigger.next();
                                return;
                            }

                            final VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class));
                            final BackupStorageInventory bsinv = selectBackupStorage(vol, msg.getVolumeActualSize());

                            if (bsinv != null) {
                                targetBackupStorageUuid = bsinv.getUuid();
                                trigger.next();
                            } else {
                                ErrorCode ec = err(
                                        SysErrors.RESOURCE_NOT_FOUND,
                                        "couldn’t find any BackupStorage that is connected and enabled for commiting volume [uuid:%s]", msg.getVolumeUuid()
                                );
                                trigger.fail(ec);
                            }

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

                            imageExtEmitter.beforeCreateImage(ImageInventory.valueOf(imageVO), targetBackupStorageUuid, msg.getPrimaryStorageUuid());

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
                        String __name__ = "do-legacy-commit-of-current-image";

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
                            doLegacyCommit(targetBackupStorageUuid, imageVO, new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
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
                            logger.debug(String.format("successfully create template[uuid:%s] from volume[uuid:%sm type:%s]",
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
    }

    public CephPrimaryStorageImageStoreBackend(PrimaryStorageVO self) {
        super(self);
    }

    @Override
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof CommitVolumeAsImageMsg) {
            handle((CommitVolumeAsImageMsg) msg);
        } else if (msg instanceof SelectBackupStorageMsg) {
            handle((SelectBackupStorageMsg) msg);
        } else if (msg instanceof ResizeVolumeOnPrimaryStorageMsg) {
            handle((ResizeVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteVolumeOnPrimaryStorageMsg) {
            handle((DeleteVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof InstantiateVolumeOnPrimaryStorageMsg) {
            handle((InstantiateVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof RevertVolumeFromSnapshotOnPrimaryStorageMsg) {
            handle((RevertVolumeFromSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof TakeSnapshotMsg) {
            handle((TakeSnapshotMsg) msg);
        } else if (msg instanceof DeleteSnapshotOnPrimaryStorageMsg) {
            handle((DeleteSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CleanImageMetaOnPrimaryStorageMsg) {
            handle((CleanImageMetaOnPrimaryStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    protected void handle(TakeSnapshotMsg msg) {
        // block volume instantiate
        BlockVolumeVO blockVolumeVO = Q.New(BlockVolumeVO.class)
                .eq(BlockVolumeVO_.uuid, msg.getStruct().getCurrent().getVolumeUuid())
                .find();
        // not block volume let super class to handle it
        if (blockVolumeVO == null) {
            super.handle(msg);
            return;
        }

        BlockPrimaryStorageFactory factory = blockMgr.getBlockPrimaryStorageFactory(blockVolumeVO.getVendor());
        BlockPrimaryStorageBackend backend = factory.getBlockPrimaryStorageBackend(self);
        backend.handle(msg);
    }

    protected void handle(final DeleteSnapshotOnPrimaryStorageMsg msg) {
        // block volume instantiate
        BlockVolumeVO blockVolumeVO = Q.New(BlockVolumeVO.class)
                .eq(BlockVolumeVO_.uuid, msg.getSnapshot().getVolumeUuid())
                .find();
        // not block volume let super class to handle it
        if (blockVolumeVO == null) {
            super.handle(msg);
            return;
        }

        BlockPrimaryStorageFactory factory = blockMgr.getBlockPrimaryStorageFactory(blockVolumeVO.getVendor());
        BlockPrimaryStorageBackend backend = factory.getBlockPrimaryStorageBackend(self);
        backend.handle(msg);
    }

    protected void handle(final RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        // block volume instantiate
        BlockVolumeVO blockVolumeVO = Q.New(BlockVolumeVO.class)
                .eq(BlockVolumeVO_.uuid, msg.getVolume().getUuid())
                .find();
        // not block volume let super class to handle it
        if (blockVolumeVO == null) {
            super.handle(msg);
            return;
        }

        BlockPrimaryStorageFactory factory = blockMgr.getBlockPrimaryStorageFactory(blockVolumeVO.getVendor());
        BlockPrimaryStorageBackend backend = factory.getBlockPrimaryStorageBackend(self);
        backend.handle(msg);
    }

    @Override
    public void handle(final InstantiateVolumeOnPrimaryStorageMsg msg) {
        // block volume instantiate
        BlockVolumeVO blockVolumeVO = Q.New(BlockVolumeVO.class)
                .eq(BlockVolumeVO_.uuid, msg.getVolume().getUuid())
                .find();
        // not block volume let super class to handle it
        if (blockVolumeVO == null) {
            super.handle(msg);
            return;
        }

        BlockPrimaryStorageFactory factory = blockMgr.getBlockPrimaryStorageFactory(blockVolumeVO.getVendor());
        BlockPrimaryStorageBackend backend = factory.getBlockPrimaryStorageBackend(self);
        backend.handle(msg);
    }

    @Override
    protected void handle(DeleteVolumeOnPrimaryStorageMsg msg) {
        BlockVolumeVO vo = Q.New(BlockVolumeVO.class)
                .eq(BlockVolumeVO_.uuid, msg.getVolume().getUuid())
                .find();
        if (vo == null) {
            super.handle(msg);
            return;
        }

        BlockPrimaryStorageFactory factory = blockMgr.getBlockPrimaryStorageFactory(vo.getVendor());
        BlockPrimaryStorageBackend backend = factory.getBlockPrimaryStorageBackend(self);
        backend.handle(msg);
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof GetAccessPathMsg) {
            handle((GetAccessPathMsg) msg);
        } else {
            super.handleMessage(msg);
        }
    }

    private void handle(GetAccessPathMsg msg) {
        String manufacturer = CephSystemTags.CEPH_MANUFACTURER.getTokenByResourceUuid(self.getUuid(), CephSystemTags.CEPH_MANUFACTURER_TOKEN);

        blockMgr.getBlockPrimaryStorageFactory(manufacturer)
                .getBlockPrimaryStorageBackend(self)
                .handle(msg);
    }

    private void handle(CleanImageMetaOnPrimaryStorageMsg msg) {
        bus.reply(msg, new CleanImageMetaOnPrimaryStorageReply());
    }

    private void handle(final ResizeVolumeOnPrimaryStorageMsg msg) {
        BlockVolumeVO vo = Q.New(BlockVolumeVO.class).eq(BlockVolumeVO_.uuid, msg.getVolume().getUuid()).find();
        if (vo != null) {
            BlockPrimaryStorageFactory factory = blockMgr.getBlockPrimaryStorageFactory(vo.getVendor());
            BlockPrimaryStorageBackend backend = factory.getBlockPrimaryStorageBackend(self);
            backend.handle(msg);
        } else {
            final ResizeVolumeOnPrimaryStorageReply reply = new ResizeVolumeOnPrimaryStorageReply();
            final VolumeInventory volume = msg.getVolume();
            ResizeVolumeCmd cmd = new ResizeVolumeCmd();
            cmd.setInstallPath(volume.getInstallPath());
            cmd.setSize(msg.getSize());
            cmd.setForce(msg.isForce());

            httpCall(RESIZE_VOLUME_PATH, cmd, ResizeVolumeRsp.class, new ReturnValueCompletion<ResizeVolumeRsp>(null) {
                @Override
                public void success(ResizeVolumeRsp returnValue) {
                    logger.debug(String.format("successfully resize the volume[uuid:%s] to %d", volume.getUuid(), returnValue.getSize()));
                    volume.setSize(returnValue.getSize());
                    reply.setVolume(volume);
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.error(String.format("fail to resize volume[uuid:%s] to %d", volume.getUuid(), msg.getSize()));
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        }
    }

    private void handle(final CommitVolumeAsImageMsg msg) {
        new CephCommitVolumeAsImageFlowChain(msg).run();
    }

    private void handle(final SelectBackupStorageMsg msg) {
        SelectBackupStorageReply reply = new SelectBackupStorageReply();
        VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class));
        reply.setInventory(new CephCommitVolumeAsImageFlowChain().selectBackupStorage(vol, msg.getRequiredSize(), msg.getRequiredBackupStorageTypes()));

        bus.reply(msg, reply);
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

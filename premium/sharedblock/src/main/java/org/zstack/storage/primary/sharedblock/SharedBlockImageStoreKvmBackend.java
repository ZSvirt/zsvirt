package org.zstack.storage.primary.sharedblock;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.CreateImageExtensionPoint;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.storage.backup.imagestore.*;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

public class SharedBlockImageStoreKvmBackend extends SharedBlockKvmBackend {
    @Autowired
    private AccountManager acntMgr;

    @Autowired
    private SharedBlockGroupPrimaryStorageFactory primaryStorageFactory;

    public SharedBlockImageStoreKvmBackend(PrimaryStorageVO self) {
        super(self);
    }

    public static final String COMMIT_PATH = "/sharedblock/imagestore/commit";
    public static final String DELETE_LV_META_PATH = "/sharedblock/lv/meta/clean";

    public static class CleanLvMetaCmd extends SharedBlockKvmCommands.AgentCmd {
        public String primaryStorageInstallPath;
    }

    public static class CommitVolumeAsImageCmd extends SharedBlockKvmCommands.AgentCmd {
        private String primaryStorageInstallPath;
        private String description;
        private String hostname; // the host name of the image store backup storage
        private String imageUuid;

        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }
    }

    public static class CommitVolumeAsImageRsp extends SharedBlockKvmCommands.AgentRsp {
        public String getBackupStorageInstallPath() {
            return backupStorageInstallPath;
        }

        public void setBackupStorageInstallPath(String backupStorageInstallPath) {
            this.backupStorageInstallPath = backupStorageInstallPath;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }

        private String backupStorageInstallPath;
        private long size;
        private long actualSize;
    }

    @Override
    public void handleLocalMessage(Message msg) {
        if (msg instanceof CommitVolumeAsImageMsg) {
            handle((CommitVolumeAsImageMsg) msg);
        } else if (msg instanceof SelectBackupStorageMsg) {
            handle((SelectBackupStorageMsg) msg);
        } else if (msg instanceof CommitVolumeAsImageOnPrimaryStorageMsg) {
            handle((CommitVolumeAsImageOnPrimaryStorageMsg) msg);
        } else if (msg instanceof ResizeVolumeOnPrimaryStorageMsg) {
            handle((ResizeVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CleanImageMetaOnPrimaryStorageMsg) {
            handle((CleanImageMetaOnPrimaryStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    private void handle(CleanImageMetaOnPrimaryStorageMsg msg) {
        CleanImageMetaOnPrimaryStorageReply reply = new CleanImageMetaOnPrimaryStorageReply();
        List<HostInventory> hosts = primaryStorageFactory.getConnectedHostsForOperation(msg.getPsUuid());
        if (hosts == null || hosts.isEmpty()) {
            throw new OperationFailureException(operr("can not find qualified kvm host for shared block group " +
                    "primary storage[uuid: %s]", msg.getPsUuid()));
        }

        CleanLvMetaCmd cmd = new CleanLvMetaCmd();
        cmd.primaryStorageInstallPath = msg.getPrimaryStorageInstallPath();
        cmd.vgUuid = msg.getPsUuid();

        httpCall(DELETE_LV_META_PATH, hosts.get(0).getUuid(), cmd, SharedBlockKvmCommands.AgentRsp.class, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(msg) {
            @Override
            public void success(SharedBlockKvmCommands.AgentRsp rsp) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(ResizeVolumeOnPrimaryStorageMsg msg) {
        VolumeInventory volume = msg.getVolume();
        final ResizeVolumeOnPrimaryStorageReply reply = new ResizeVolumeOnPrimaryStorageReply();

        List<HostInventory> hosts = primaryStorageFactory.getConnectedHostsForOperation(volume.getPrimaryStorageUuid());
        if (hosts == null || hosts.isEmpty()) {
            return;
        }

        SharedBlockKvmCommands.ResizeVolumeCmd cmd = new SharedBlockKvmCommands.ResizeVolumeCmd();
        cmd.installPath = volume.getInstallPath();
        cmd.size = msg.getSize();
        cmd.vgUuid = volume.getPrimaryStorageUuid();
        cmd.live = false;
        cmd.force = msg.isForce();
        cmd.volumeUuid = volume.getUuid();
        httpCall(SharedBlockKvmCommands.RESIZE_VOLUME_PATH, hosts.get(0).getUuid(), cmd, SharedBlockKvmCommands.ResizeVolumeRsp.class, new ReturnValueCompletion<SharedBlockKvmCommands.ResizeVolumeRsp>(msg) {
            @Override
            public void success(SharedBlockKvmCommands.ResizeVolumeRsp rsp) {
                volume.setSize(rsp.size);
                reply.setVolume(volume);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void commitSnapshot(final CommitVolumeAsImageOnPrimaryStorageMsg msg, final String installPath, final ReturnValueCompletion<CommitVolumeAsImageRsp> completion) {
        ImageInventory inv = ImageInventory.valueOf(dbf.findByUuid(msg.getImageUuid(), ImageVO.class));
        String hostUuid = primaryStorageFactory.getConnectedHostsForOperation(getSelfInventory()).get(0).getUuid();
        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();
        if (volumeVO.isAttached() && volumeVO.getVmInstanceUuid() != null) {
            VmInstanceVO instanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, volumeVO.getVmInstanceUuid()).find();
            if (instanceVO.getHostUuid() != null) {
                hostUuid = instanceVO.getHostUuid();
            }
        }

        // get the hostname of the backup storage
        SimpleQuery<ImageStoreBackupStorageVO> q2 = this.dbf.createQuery(ImageStoreBackupStorageVO.class);
        q2.select(ImageStoreBackupStorageVO_.hostname);
        q2.add(ImageStoreBackupStorageVO_.uuid, SimpleQuery.Op.EQ, msg.getBackupStorageUuid());
        String hostname = q2.findValue();

        CommitVolumeAsImageCmd cmd = new CommitVolumeAsImageCmd();
        cmd.setPrimaryStorageInstallPath(installPath);
        cmd.setHostname(hostname);
        cmd.setImageUuid(msg.getImageUuid());

        StringBuilder desc = new StringBuilder();
        for (CreateImageExtensionPoint ext : pluginRgty.getExtensionList(CreateImageExtensionPoint.class)) {
            String tmp = ext.getImageDescription(inv);
            if (tmp != null && !tmp.trim().equals("")) {
                desc.append(tmp);
            }
        }
        cmd.setDescription(desc.toString());

        // send the command to agent
        this.httpCall(COMMIT_PATH, hostUuid, cmd, CommitVolumeAsImageRsp.class, new ReturnValueCompletion<CommitVolumeAsImageRsp>(completion) {
            @Override
            public void success(CommitVolumeAsImageRsp rsp) {
                completion.success(rsp);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void handle(final SelectBackupStorageMsg msg) {
        SelectBackupStorageReply reply = new SelectBackupStorageReply();
        VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class));
        reply.setInventory(new CommitVolumeAsImageFlowChain().selectBackupStorage(vol, msg.getRequiredSize(), msg.getRequiredBackupStorageTypes()));

        bus.reply(msg, reply);
    }

    private void handle(final CommitVolumeAsImageMsg msg) {
        CommitVolumeAsImageFlowChain.getFlow(msg).run();
    }

    private void handle(final CommitVolumeAsImageOnPrimaryStorageMsg msg) {
        // For image store, we just need to push the image to store.
        final CommitVolumeAsImageOnPrimaryStorageReply reply = new CommitVolumeAsImageOnPrimaryStorageReply();
        reply.setBackupStorageUuid(msg.getBackupStorageUuid());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("commit-volume-%s-as-image", msg.getVolumeUuid()));
        chain.enableProgressReport();
        chain.then(new ShareFlow() {
            String backupStorageInstallPath;
            CommitVolumeAsImageRsp cvRsp;
            String volumeSnapshotInstallPath;
            String volumeSnapshotFormat;
            VolumeSnapshotInventory snapshot;

            // The flow logic:
            // 1. Create a live snapshot (d2) of current image (d1)
            // 2. Push d1 to image store.
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "take-live-snapshot-of-current-image";

                    @Override
                    public boolean skip(Map data) {
                        if (msg instanceof CommitVolumeSnapshotAsImageOnPrimaryStorageMsg) {
                            CommitVolumeSnapshotAsImageOnPrimaryStorageMsg vsmsg = (CommitVolumeSnapshotAsImageOnPrimaryStorageMsg) msg;
                            VolumeSnapshotVO snapshotVO = dbf.findByUuid(vsmsg.getSnapshotUuid(), VolumeSnapshotVO.class);
                            volumeSnapshotInstallPath = snapshotVO.getPrimaryStorageInstallPath();
                            volumeSnapshotFormat = snapshotVO.getFormat();
                            snapshot = VolumeSnapshotInventory.valueOf(snapshotVO);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        VolumeCreateSnapshotMsg cmsg = new VolumeCreateSnapshotMsg();
                        String volUuid = msg.getVolumeUuid();
                        cmsg.setVolumeUuid(volUuid);
                        cmsg.setName("Snapshot-" + volUuid);
                        cmsg.setDescription("Take snapshot for " + volUuid);
                        cmsg.setAccountUuid(acntMgr.getOwnerAccountUuidOfResource(volUuid));
                        if (PrimaryStorageGlobalConfig.UNDO_TEMP_SNAPSHOT.value(Boolean.class)) {
                            cmsg.setRequiredSnapshotMode(SnapshotMode.INCREMENTAL);
                            cmsg.setQueuedInVolume(false);
                        }
                        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeConstant.SERVICE_ID, volUuid);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                VolumeCreateSnapshotReply vsReply = reply.castReply();
                                snapshot = vsReply.getInventory();

                                SharedBlockPrimaryStorageCanonicalEvents.ImageInnerSnapshotCreated imageInnerSnapshotCreated = new SharedBlockPrimaryStorageCanonicalEvents.ImageInnerSnapshotCreated();
                                imageInnerSnapshotCreated.imageUuid = msg.getImageUuid();
                                imageInnerSnapshotCreated.primaryStorageUuid = msg.getPrimaryStorageUuid();
                                imageInnerSnapshotCreated.snapshot = vsReply.getInventory();
                                imageInnerSnapshotCreated.fire();

                                volumeSnapshotInstallPath = vsReply.getInventory().getPrimaryStorageInstallPath();
                                volumeSnapshotFormat = vsReply.getInventory().getFormat();

                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (snapshot == null || !PrimaryStorageGlobalConfig.UNDO_TEMP_SNAPSHOT.value(Boolean.class)) {
                            trigger.rollback();
                            return;
                        }

                        VolumeSnapshotDeletionMsg dmsg = new VolumeSnapshotDeletionMsg();
                        dmsg.setTreeUuid(snapshot.getTreeUuid());
                        dmsg.setVolumeUuid(snapshot.getVolumeUuid());
                        dmsg.setSnapshotUuid(snapshot.getUuid());
                        dmsg.setDirection(DeleteVolumeSnapshotDirection.Commit.toString());
                        dmsg.setScope(DeleteVolumeSnapshotScope.Single.toString());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeSnapshotConstant.SERVICE_ID, snapshot.getUuid());
                        bus.send(dmsg, new CloudBusCallBack(msg) {
                            @Override
                            public void run(MessageReply reply) {
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "commit-snapshot-to-local-image-registry";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        commitSnapshot(msg, volumeSnapshotInstallPath, new ReturnValueCompletion<CommitVolumeAsImageRsp>(trigger) {
                            @Override
                            public void success(CommitVolumeAsImageRsp returnValue) {
                                cvRsp = returnValue;
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
                    String __name__ = "upload-template-to-backup-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        String hostUuid = primaryStorageFactory.getConnectedHostsForOperation(getSelfInventory()).get(0).getUuid();
                        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();
                        if (volumeVO.isAttached() && volumeVO.getVmInstanceUuid() != null) {
                            VmInstanceVO instanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, volumeVO.getVmInstanceUuid()).find();
                            if (instanceVO.getHostUuid() != null) {
                                hostUuid = instanceVO.getHostUuid();
                            }
                        }

                        ImageStoreBackupStorageKvmUploader uploader = ImageStoreBackupStorageKvmUploader.createUploader(getSelfInventory(), msg.getBackupStorageUuid());
                        String primaryStorageInstallPath = volumeSnapshotInstallPath;
                        uploader.uploadBits(msg.getImageUuid(), backupStorageInstallPath, primaryStorageInstallPath, hostUuid, new ReturnValueCompletion<String>(trigger) {
                            @Override
                            public void success(String installPath) {
                                backupStorageInstallPath = installPath;
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
                    String __name__ = "delete-temp-snapshot";

                    @Override
                    public boolean skip(Map data) {
                        return msg instanceof CommitVolumeSnapshotAsImageOnPrimaryStorageMsg ||
                                !PrimaryStorageGlobalConfig.UNDO_TEMP_SNAPSHOT.value(Boolean.class);
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VolumeSnapshotDeletionMsg dmsg = new VolumeSnapshotDeletionMsg();
                        dmsg.setTreeUuid(snapshot.getTreeUuid());
                        dmsg.setVolumeUuid(snapshot.getVolumeUuid());
                        dmsg.setSnapshotUuid(snapshot.getUuid());
                        dmsg.setScope(DeleteVolumeSnapshotScope.Single.toString());
                        dmsg.setDirection(DeleteVolumeSnapshotDirection.Commit.toString());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeSnapshotConstant.SERVICE_ID, snapshot.getUuid());
                        bus.send(dmsg, new CloudBusCallBack(msg) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        reply.setBackupStorageInstallPath(backupStorageInstallPath);
                        reply.setFormat(volumeSnapshotFormat);
                        reply.setSize(cvRsp.getSize());
                        reply.setActualSize(cvRsp.getActualSize());
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }
}

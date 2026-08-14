package org.zstack.storage.primary.imagestore.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.HasThreadContext;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.CreateImageExtensionPoint;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KVMConstant;
import org.zstack.storage.backup.imagestore.*;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.storage.primary.local.*;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class LocalStorageImageStoreKvmBackend extends LocalStorageKvmBackend {
    @Autowired
    private LocalStorageFactory localStorageFactory;

    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private PluginRegistry pluginRgty;

    public static final String COMMIT_PATH = "/localstorage/imagestore/commit";
    public static final String CLEAN_IMAGE_META_PATH = "/localstorage/imagestore/meta/clean";
    public static final String RESIZE_VOLUME_PATH = "/localstorage/volume/resize";

    public static class CleanImageMetaCmd extends AgentCommand {
        private String primaryStorageInstallPath;

        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }
    }

    public static class CommitVolumeAsImageCmd extends AgentCommand implements HasThreadContext{
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

    public static class CommitVolumeAsImageRsp extends AgentResponse {
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

    public LocalStorageImageStoreKvmBackend() {
    }

    public LocalStorageImageStoreKvmBackend(PrimaryStorageVO self) {
        super(self);
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

    protected void handle(CleanImageMetaOnPrimaryStorageMsg msg) {
        CleanImageMetaOnPrimaryStorageReply reply = new CleanImageMetaOnPrimaryStorageReply();
        CleanImageMetaCmd cmd = new CleanImageMetaCmd();
        cmd.setPrimaryStorageInstallPath(msg.getPrimaryStorageInstallPath());
        String hostUuid = getHostUuidByResourceUuid(msg.getVolumeUuid());

        httpCall(CLEAN_IMAGE_META_PATH, hostUuid, cmd, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(msg) {
            @Override
            public void success(AgentResponse rsp) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final ResizeVolumeOnPrimaryStorageMsg msg) {
        VolumeInventory volume = msg.getVolume();

        String volumeUuid = volume.getUuid();
        String hostUuid = getHostUuidByResourceUuid(volumeUuid);
        String primaryStorageUuid = msg.getPrimaryStorageUuid();

        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("resize-volume-%s-on-primary-storage-%s", volumeUuid, primaryStorageUuid));
        chain.then(new NoRollbackFlow() {
            String __name__ = "resize-volume-on-host";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ResizeVolumeCmd cmd = new ResizeVolumeCmd();
                cmd.setInstallPath(volume.getInstallPath());
                cmd.setSize(msg.getSize());
                cmd.setForce(msg.isForce());

                httpCall(RESIZE_VOLUME_PATH, hostUuid, cmd, ResizeVolumeRsp.class, new ReturnValueCompletion<ResizeVolumeRsp>(trigger) {
                    @Override
                    public void success(ResizeVolumeRsp rsp) {
                        SQL.New(LocalStorageResourceRefVO.class)
                                .condAnd(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, volume.getUuid())
                                .condAnd(LocalStorageResourceRefVO_.hostUuid, Op.EQ, hostUuid)
                                .set(LocalStorageResourceRefVO_.size, msg.getSize())
                                .update();
                        volume.setSize(rsp.getSize());
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                ResizeVolumeOnPrimaryStorageReply reply = new ResizeVolumeOnPrimaryStorageReply();
                reply.setVolume(volume);
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                ResizeVolumeOnPrimaryStorageReply reply = new ResizeVolumeOnPrimaryStorageReply();
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    private String getVolumeRootImageUuid(String volumeUuid) {
        SimpleQuery<VolumeVO> q = this.dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.rootImageUuid);
        q.add(VolumeVO_.uuid, Op.EQ, volumeUuid);
        return q.findValue();
    }

    private void commitSnapshot(final CommitVolumeAsImageOnPrimaryStorageMsg msg, final String installPath, final ReturnValueCompletion<CommitVolumeAsImageRsp> completion) {
        ImageInventory inv = ImageInventory.valueOf(dbf.findByUuid(msg.getImageUuid(), ImageVO.class));
        String hostUuid = getHostUuidByResourceUuid(msg.getVolumeUuid());
        String rootImageUuid = getVolumeRootImageUuid(msg.getVolumeUuid());

        // get the hostname of the backup storage
        SimpleQuery<ImageStoreBackupStorageVO> q2 = this.dbf.createQuery(ImageStoreBackupStorageVO.class);
        q2.select(ImageStoreBackupStorageVO_.hostname);
        q2.add(ImageStoreBackupStorageVO_.uuid, Op.EQ, msg.getBackupStorageUuid());
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

    private void handle(final CommitVolumeAsImageMsg msg) {
        CommitVolumeAsImageFlowChain.getFlow(msg).run();
    }

    private void handle(final SelectBackupStorageMsg msg) {
        SelectBackupStorageReply reply = new SelectBackupStorageReply();
        VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class));
        reply.setInventory(new CommitVolumeAsImageFlowChain().selectBackupStorage(vol, msg.getRequiredSize(), msg.getRequiredBackupStorageTypes()));

        bus.reply(msg, reply);
    }

    private void handle(final CommitVolumeAsImageOnPrimaryStorageMsg msg) {
        // For image store, we just need to push the image to store.
        final LocalStorageResourceRefVO ref = Q.New(LocalStorageResourceRefVO.class)
                .eq(LocalStorageResourceRefVO_.resourceUuid, msg.getVolumeUuid())
                .find();
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

                                LocalPrimaryStorageCanonicalEvents.ImageInnerSnapshotCreated imageInnerSnapshotCreated = new LocalPrimaryStorageCanonicalEvents.ImageInnerSnapshotCreated();
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
                        BackupStorageVO bsvo = dbf.findByUuid(msg.getBackupStorageUuid(), BackupStorageVO.class);
                        LocalStorageBackupStorageMediator m = localStorageFactory.getBackupStorageMediator(KVMConstant.KVM_HYPERVISOR_TYPE, bsvo.getType());
                        String primaryStorageInstallPath = volumeSnapshotInstallPath;
                        m.uploadBits(msg.getImageUuid(), getSelfInventory(), BackupStorageInventory.valueOf(bsvo), backupStorageInstallPath, primaryStorageInstallPath, ref.getHostUuid(), new ReturnValueCompletion<String>(trigger) {
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

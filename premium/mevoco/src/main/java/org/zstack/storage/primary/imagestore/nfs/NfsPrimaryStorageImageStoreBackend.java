package org.zstack.storage.primary.imagestore.nfs;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.HasThreadContext;
import org.zstack.header.agent.AgentResponse;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.image.CreateImageExtensionPoint;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.backup.imagestore.*;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.storage.primary.nfs.NfsPrimaryStorage;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageFactory;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands;
import org.zstack.storage.primary.nfs.NfsPrimaryToBackupStorageMediator;

import java.util.Map;

import static org.zstack.core.Platform.operr;

public class NfsPrimaryStorageImageStoreBackend extends NfsPrimaryStorage {
    @Autowired
    private NfsPrimaryStorageFactory nfsPrimaryStorageFactory;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    private PluginRegistry pluginRgty;

    public static final String COMMIT_PATH = "/nfsprimarystorage/imagestore/commit";
    public static final String RESIZE_VOLUME_PATH = "/nfsprimarystorage/volume/resize";
    public static final String CLEAN_IMAGE_META_PATH = "/nfsprimarystorage/imagestore/meta/clean";

    public NfsPrimaryStorageImageStoreBackend() {
    }

    public static class CommitVolumeAsImageCmd extends ImageStoreBackupStorageCommands.AgentCommand implements HasThreadContext{
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

    public static class CleanImageMetaCmd extends NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentCommand {
        private String primaryStorageInstallPath;

        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }
    }

    public static class ResizeVolumeCmd extends NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentCommand {
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

    public static class ResizeVolumeRsp extends NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse {
        private long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public NfsPrimaryStorageImageStoreBackend(PrimaryStorageVO self) {
        super(self);
    }

    @Override
    protected void handleLocalMessage(Message msg) {
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
        String hostUuid = nfsPrimaryStorageFactory.getConnectedHostForOperation(getSelfInventory()).get(0).getUuid();

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(hostUuid);
        kmsg.setPath(CLEAN_IMAGE_META_PATH);
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply messageReply) {
                if (!messageReply.isSuccess()) {
                    reply.setError(messageReply.getError());
                    bus.reply(msg, reply);
                    return;
                }

                KVMHostAsyncHttpCallReply r = messageReply.castReply();

                if (!r.isSuccess()) {
                    reply.setError(operr("operation error, because:%s", r.getError()));
                }

                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final ResizeVolumeOnPrimaryStorageMsg msg) {
        VolumeInventory volume = msg.getVolume();

        String hostUuid = nfsPrimaryStorageFactory.getConnectedHostForOperation(getSelfInventory()).get(0).getUuid();

        final ResizeVolumeOnPrimaryStorageReply reply = new ResizeVolumeOnPrimaryStorageReply();

        ResizeVolumeCmd cmd = new ResizeVolumeCmd();
        cmd.setInstallPath(volume.getInstallPath());
        cmd.setSize(msg.getSize());
        cmd.setForce(msg.isForce());

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(hostUuid);
        kmsg.setPath(RESIZE_VOLUME_PATH);
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply messageReply) {
                if (!messageReply.isSuccess()) {
                    reply.setError(messageReply.getError());
                    bus.reply(msg, reply);
                    return;
                }

                KVMHostAsyncHttpCallReply r = messageReply.castReply();
                ResizeVolumeRsp rsp = r.toResponse(ResizeVolumeRsp.class);
                if (!rsp.isSuccess()) {
                    reply.setError(operr("operation error, because:%s", rsp.getError()));
                } else {
                    volume.setSize(rsp.getSize());
                    reply.setVolume(volume);
                }

                bus.reply(msg, reply);
            }
        });
    }

    private String getVolumeRootImageUuid(String volumeUuid) {
        SimpleQuery<VolumeVO> q = this.dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.rootImageUuid);
        q.add(VolumeVO_.uuid, SimpleQuery.Op.EQ, volumeUuid);
        return q.findValue();
    }

    private void commitSnapshot(final CommitVolumeAsImageOnPrimaryStorageMsg msg, final String installPath, final ReturnValueCompletion<CommitVolumeAsImageRsp> completion) {
        ImageInventory inv = ImageInventory.valueOf(dbf.findByUuid(msg.getImageUuid(), ImageVO.class));
        String hostUuid = nfsPrimaryStorageFactory.getConnectedHostForOperation(getSelfInventory()).get(0).getUuid();
        String rootImageUuid = getVolumeRootImageUuid(msg.getVolumeUuid());

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

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(hostUuid);
        kmsg.setPath(COMMIT_PATH);
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                CommitVolumeAsImageRsp rsp = r.toResponse(CommitVolumeAsImageRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("operation error, because:%s", rsp.getError()));
                } else {
                    completion.success(rsp);
                }
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
                        NfsPrimaryToBackupStorageMediator m = nfsPrimaryStorageFactory.getPrimaryToBackupStorageMediator(
                                BackupStorageType.valueOf(bsvo.getType()),
                                HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE));
                        String primaryStorageInstallPath = volumeSnapshotInstallPath;
                        m.uploadBits(msg.getImageUuid(), getSelfInventory(), BackupStorageInventory.valueOf(bsvo), backupStorageInstallPath, primaryStorageInstallPath, new ReturnValueCompletion<String>(trigger) {
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

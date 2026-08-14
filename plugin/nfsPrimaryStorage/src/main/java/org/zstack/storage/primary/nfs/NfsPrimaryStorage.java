package org.zstack.storage.primary.nfs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.asyncbatch.LoopAsyncBatch;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.storage.snapshot.ShrinkVolumeSnapshotOnPrimaryStorageMsg;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.metadata.*;
import org.zstack.header.volume.*;
import org.zstack.kvm.KVMConstant;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageMsg;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageReply;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.storage.snapshot.reference.VolumeSnapshotReferenceUtils;
import org.zstack.storage.volume.VolumeErrors;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

public class NfsPrimaryStorage extends PrimaryStorageBase {
    private static final CLogger logger = Utils.getLogger(NfsPrimaryStorage.class);

    @Autowired
    protected NfsPrimaryStorageFactory factory;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private NfsPrimaryStorageManager nfsMgr;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private NfsPrimaryStorageImageCacheCleaner imageCacheCleaner;
    @Autowired
    private PluginRegistry pluginRgty;

    public NfsPrimaryStorage() {
    }

    public NfsPrimaryStorage(PrimaryStorageVO vo) {
        super(vo);
    }

    @Override
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof PrimaryStorageRemoveCachedImageMsg) {
            handle((PrimaryStorageRemoveCachedImageMsg) msg);
        } else if (msg instanceof TakeSnapshotMsg) {
            handle((TakeSnapshotMsg) msg);
        } else if (msg instanceof CheckSnapshotMsg) {
            handle((CheckSnapshotMsg) msg);
        } else if (msg instanceof BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) {
            handle((BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) msg);
        } else if (msg instanceof CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CreateTemporaryVolumeFromSnapshotMsg) {
            handle((CreateTemporaryVolumeFromSnapshotMsg) msg);
        } else if (msg instanceof UploadBitsToBackupStorageMsg) {
            handle((UploadBitsToBackupStorageMsg) msg);
        } else if (msg instanceof GetVolumeRootImageUuidFromPrimaryStorageMsg) {
            handle((GetVolumeRootImageUuidFromPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteImageCacheOnPrimaryStorageMsg) {
            handle((DeleteImageCacheOnPrimaryStorageMsg) msg);
        } else if (msg instanceof NfsRecalculatePrimaryStorageCapacityMsg) {
            handle((NfsRecalculatePrimaryStorageCapacityMsg) msg);
        } else if (msg instanceof NfsToNfsMigrateBitsMsg) {
            handle((NfsToNfsMigrateBitsMsg) msg);
        } else if (msg instanceof NfsRebaseVolumeBackingFileMsg) {
            handle((NfsRebaseVolumeBackingFileMsg) msg);
        } else if (msg instanceof DownloadBitsFromKVMHostToPrimaryStorageMsg) {
            handle((DownloadBitsFromKVMHostToPrimaryStorageMsg) msg);
        } else if (msg instanceof CancelDownloadBitsFromKVMHostToPrimaryStorageMsg) {
            handle((CancelDownloadBitsFromKVMHostToPrimaryStorageMsg) msg);
        } else if ((msg instanceof GetDownloadBitsFromKVMHostProgressMsg)) {
            handle((GetDownloadBitsFromKVMHostProgressMsg) msg);
        } else if (msg instanceof GetVolumeBackingChainFromPrimaryStorageMsg) {
            handle((GetVolumeBackingChainFromPrimaryStorageMsg) msg);
        } else if (msg instanceof CommitVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CommitVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof PullVolumeSnapshotOnPrimaryStorageMsg) {
            handle((PullVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof RebaseVolumeBackingFileOnPrimaryStorageMsg) {
            handle((RebaseVolumeBackingFileOnPrimaryStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    protected void updateMountPoint(String newUrl, Completion completion) {
        String oldUrl = self.getUrl();

        SimpleQuery<PrimaryStorageClusterRefVO> q = dbf.createQuery(PrimaryStorageClusterRefVO.class);
        q.select(PrimaryStorageClusterRefVO_.clusterUuid);
        q.add(PrimaryStorageClusterRefVO_.primaryStorageUuid, Op.EQ, self.getUuid());
        List<String> cuuids = q.listValue();
        if (cuuids.isEmpty()) {
            completion.success();
        }

        for (UpdatePrimaryStorageExtensionPoint ext : pluginRgty.getExtensionList(UpdatePrimaryStorageExtensionPoint.class)) {
            ext.beforeUpdatePrimaryStorage(PrimaryStorageInventory.valueOf(self));
        }

        PrimaryStorageInventory psinv = getSelfInventory();
        new LoopAsyncBatch<String>(completion) {
            @Override
            protected Collection<String> collect() {
                return cuuids;
            }

            @Override
            protected AsyncBatchRunner forEach(String item) {
                return new AsyncBatchRunner() {
                    @Override
                    public void run(NoErrorCompletion completion) {
                        NfsPrimaryStorageBackend bkd = getBackendByClusterUuid(item);
                        bkd.updateMountPoint(psinv, item, oldUrl, newUrl, new Completion(completion) {
                            @Override
                            public void success() {
                                completion.done();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.warn(String.format("failed to update the nfs[uuid:%s, name:%s] mount point" +
                                                " from %s to %s in the cluster[uuid:%s], %s", self.getUuid(), self.getName(),
                                        oldUrl, newUrl, item, errorCode));
                                errors.add(errorCode);
                                completion.done();
                            }
                        });
                    }
                };
            }

            @Override
            protected void done() {
                if (errors.size() == cuuids.size()) {
                    completion.fail(errors.get(0));
                } else {
                    completion.success();
                }
            }
        }.start();
    }

    @Override
    protected void updatePrimaryStorage(APIUpdatePrimaryStorageMsg msg, ReturnValueCompletion<PrimaryStorageInventory> completion) {
        if (msg.getUrl() != null && !self.getUrl().equals(msg.getUrl())) {
            thdf.chainSubmit(new ChainTask(msg) {

                @Override
                public String getSyncSignature() {
                    return getSyncId();
                }

                @Override
                public String getName() {
                    return String.format("update-primary-storage-%s", self.getUuid());
                }

                @Override
                public void run(SyncTaskChain chain) {
                    updateMountPoint(msg.getUrl(), new Completion(completion) {
                        @Override
                        public void success() {
                            NfsPrimaryStorage.super.updatePrimaryStorage(msg, completion);
                            for (UpdatePrimaryStorageExtensionPoint ext : pluginRgty.getExtensionList(UpdatePrimaryStorageExtensionPoint.class)) {
                                ext.afterUpdatePrimaryStorage(PrimaryStorageInventory.valueOf(self));
                            }
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                            chain.next();
                        }
                    });
                }
            });
        } else {
            super.updatePrimaryStorage(msg, completion);
        }
    }

    @Override
    protected void handle(APICleanUpImageCacheOnPrimaryStorageMsg msg) {
        APICleanUpImageCacheOnPrimaryStorageEvent evt = new APICleanUpImageCacheOnPrimaryStorageEvent(msg.getId());
        imageCacheCleaner.cleanup(msg.getUuid(), false);
        bus.publish(evt);
    }

    private void handle(final DeleteImageCacheOnPrimaryStorageMsg msg) {
        NfsPrimaryStorageBackend bkd = getUsableBackend();
        if (bkd == null) {
            throw new OperationFailureException(operr("cannot find usable backend"));
        }
        DeleteImageCacheOnPrimaryStorageReply sreply = new DeleteImageCacheOnPrimaryStorageReply();
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-image-cache-on-nfs-primary-storage-%s", msg.getPrimaryStorageUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "delete-volume-cache";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                DeleteVolumeBitsOnPrimaryStorageMsg dmsg = new DeleteVolumeBitsOnPrimaryStorageMsg();
                dmsg.setFolder(true);
                dmsg.setHypervisorType(bkd.getHypervisorType().toString());
                dmsg.setInstallPath(new File(msg.getInstallPath()).getParent());
                dmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, sreply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                sreply.setError(errCode);
                bus.reply(msg, sreply);
            }
        }).start();
    }

    private String selectRandomHostFromPS(PrimaryStorageInventory ps) {
        List<String> cuuids = ps.getAttachedClusterUuids();
        if (cuuids.isEmpty()) {
            return null;
        }

        List<String> hosts = SQL.New("select host.uuid from ClusterVO cluster, HostVO host " +
                "where cluster.uuid = host.clusterUuid and host.status = :hostStatus and cluster.uuid in (:cuuids) order by rand()").
                param("hostStatus", HostStatus.Connected).param("cuuids", cuuids).list();
        if (hosts == null || hosts.size() == 0) {
            return null;
        }
        return hosts.get(0);
    }

    private void handle(final GetVolumeRootImageUuidFromPrimaryStorageMsg msg) {
        NfsPrimaryStorageBackend bkd = getUsableBackend();
        if (bkd == null) {
            throw new OperationFailureException(operr("no usable backend found"));
        }

        bkd.handle(getSelfInventory(), msg, new ReturnValueCompletion<GetVolumeRootImageUuidFromPrimaryStorageReply>(msg) {
            @Override
            public void success(GetVolumeRootImageUuidFromPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                GetVolumeRootImageUuidFromPrimaryStorageReply reply = new GetVolumeRootImageUuidFromPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final UploadBitsToBackupStorageMsg msg) {
        NfsPrimaryStorageBackend bkd = getBackend(HypervisorType.valueOf(msg.getHypervisorType()));
        bkd.handle(getSelfInventory(), msg, new ReturnValueCompletion<UploadBitsToBackupStorageReply>(msg) {
            @Override
            public void success(UploadBitsToBackupStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                UploadBitsToBackupStorageReply reply = new UploadBitsToBackupStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final CreateTemporaryVolumeFromSnapshotMsg msg) {
        NfsPrimaryStorageBackend bkd = getBackend(HypervisorType.valueOf(msg.getHypervisorType()));
        bkd.handle(getSelfInventory(), msg, new ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply>(msg) {
            @Override
            public void success(CreateTemporaryVolumeFromSnapshotReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateTemporaryVolumeFromSnapshotReply reply = new CreateTemporaryVolumeFromSnapshotReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    public void deleteHook() {
        List<String> cuuids = transformAndRemoveNull(self.getAttachedClusterRefs(), PrimaryStorageClusterRefVO::getClusterUuid);

        for (String cuuid : cuuids) {
            NfsPrimaryStorageBackend backend = getBackendByClusterUuid(cuuid);
            try {
                backend.detachFromCluster(getSelfInventory(), cuuid);
            } catch (NfsPrimaryStorageException e) {
                logger.warn(String.format("failed to detach the nfs primary storage[uuid: %s] from the cluster[uuid: %s]",
                        self.getUuid(), cuuid));
            }
        }
    }

    protected void handle(final MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
        final MergeVolumeSnapshotOnPrimaryStorageReply reply = new MergeVolumeSnapshotOnPrimaryStorageReply();

        VolumeSnapshotInventory snapshot = msg.getFrom();
        VolumeInventory volume = msg.getTo();
        final NfsPrimaryStorageBackend backend = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(snapshot.getFormat(), self.getUuid()));
        backend.mergeSnapshotToVolume(getSelfInventory(), snapshot, volume, msg.isFullRebase(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(FlattenVolumeOnPrimaryStorageMsg msg) {
        final FlattenVolumeOnPrimaryStorageReply reply = new FlattenVolumeOnPrimaryStorageReply();
        final NfsPrimaryStorageBackend backend = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(msg.getVolume().getFormat(), self.getUuid()));
        backend.mergeSnapshotToVolume(getSelfInventory(), null, msg.getVolume(), true, new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        final NfsPrimaryStorageBackend backend = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(msg.getSnapshot().getFormat(), self.getUuid()));
        backend.handle(getSelfInventory(), msg, new ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg) {
        BackupStorageInventory bs = msg.getBackupStorage();
        VolumeSnapshotInventory sinv = msg.getSnapshot();
        NfsPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(
                BackupStorageType.valueOf(bs.getType()),
                nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(sinv.getFormat(), self.getUuid())
        );

        final BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply reply = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply();
        final String installPath = mediator.makeVolumeSnapshotInstallPath(bs.getUuid(), sinv.getUuid());
        mediator.uploadBits(null, getSelfInventory(), bs, installPath, sinv.getPrimaryStorageInstallPath(), new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String installPath) {
                reply.setBackupStorageInstallPath(installPath);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(final RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        final RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
        HostInventory destHost;
        try {
            destHost = factory.getConnectedHostForOperation(PrimaryStorageInventory.valueOf(self)).get(0);
        } catch (OperationFailureException e) {
            reply.setError(operr("no host in Connected status to which nfs primary storage[uuid:%s, name:%s] attached" +
                            " found to revert volume[uuid:%s] to snapshot[uuid:%s, name:%s]",
                    self.getUuid(), self.getName(), msg.getVolume().getUuid(),
                    msg.getSnapshot().getUuid(), msg.getSnapshot().getName()));

            bus.reply(msg, reply);
            return;
        }

        NfsPrimaryStorageBackend bkd = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(msg.getSnapshot().getFormat(), self.getUuid()));
        bkd.revertVolumeFromSnapshot(msg.getSnapshot(), msg.getVolume(), destHost, new ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(RevertVolumeFromSnapshotOnPrimaryStorageReply returnValue) {
                reply.setNewVolumeInstallPath(returnValue.getNewVolumeInstallPath());
                reply.setSize(returnValue.getSize());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(final ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        final ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();

        HostInventory destHost = factory.getConnectedHostForOperation(PrimaryStorageInventory.valueOf(self)).get(0);
        if (destHost == null) {
            reply.setError(operr("no host in Connected status to which nfs primary storage[uuid:%s, name:%s] attached" +
                            " found to revert volume[uuid:%s] to image[uuid:%s]",
                    self.getUuid(), self.getName(),
                    msg.getVolume().getUuid(), msg.getVolume().getRootImageUuid()));

            bus.reply(msg, reply);
            return;
        }

        NfsPrimaryStorageBackend bkd = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(
                VolumeConstant.VOLUME_FORMAT_QCOW2, self.getUuid())
        );
        bkd.resetRootVolumeFromImage(msg.getVolume(), destHost, new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String returnValue) {
                reply.setNewVolumeInstallPath(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DeleteSnapshotOnPrimaryStorageMsg msg) {
        final DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
        final VolumeSnapshotInventory sinv = msg.getSnapshot();
        final NfsPrimaryStorageBackend bkd = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(sinv.getFormat(), self.getUuid()));

        bkd.delete(getSelfInventory(), sinv.getPrimaryStorageInstallPath(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                NfsDeleteVolumeSnapshotGC gc = new NfsDeleteVolumeSnapshotGC();
                gc.NAME = String.format("gc-nfs-%s-snapshot-%s", self.getUuid(), sinv.getUuid());
                gc.snapshot = sinv;
                gc.primaryStorageUuid = self.getUuid();
                gc.hypervisorType = bkd.getHypervisorType().toString();
                gc.submit(NfsPrimaryStorageGlobalConfig.GC_INTERVAL.value(Long.class), TimeUnit.SECONDS);

                logger.warn(String.format("NFS primary storage[uuid:%s] failed to delete a volume snapshot[uuid:%s], %s. A GC" +
                                " job[uuid:%s] is scheduled to cleanup it in the interval of %s seconds",
                        self.getUuid(), sinv.getUuid(), errorCode, gc.getUuid(), NfsPrimaryStorageGlobalConfig.GC_INTERVAL.value(Long.class)));
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final CheckSnapshotMsg msg) {
        final CheckSnapshotReply reply = new CheckSnapshotReply();

        VolumeVO vol = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);

        String huuid;
        String connectedHostUuid = factory.getConnectedHostForOperation(getSelfInventory()).get(0).getUuid();
        if (vol.getVmInstanceUuid() != null) {
            Tuple t = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.state, VmInstanceVO_.hostUuid)
                    .eq(VmInstanceVO_.uuid, vol.getVmInstanceUuid())
                    .findTuple();
            VmInstanceState state = t.get(0, VmInstanceState.class);
            String vmHostUuid = t.get(1, String.class);

            if (state == VmInstanceState.Running || state == VmInstanceState.Paused) {
                DebugUtils.Assert(vmHostUuid != null,
                        String.format("vm[uuid:%s] is Running or Paused, but has no hostUuid", vol.getVmInstanceUuid()));
                huuid = vmHostUuid;
            } else if (state == VmInstanceState.Stopped) {
                huuid = connectedHostUuid;
            } else {
                reply.setError(operr("vm[uuid:%s] is not Running, Paused or Stopped, current state is %s",
                        vol.getVmInstanceUuid(), state));
                bus.reply(msg, reply);
                return;
            }
        } else {
            huuid = connectedHostUuid;
        }

        CheckSnapshotOnHypervisorMsg hmsg = new CheckSnapshotOnHypervisorMsg();
        hmsg.setHostUuid(huuid);
        hmsg.setVmUuid(vol.getVmInstanceUuid());
        hmsg.setVolumeUuid(vol.getUuid());
        hmsg.setVolumeChainToCheck(msg.getVolumeChainToCheck());
        hmsg.setCurrentInstallPath(vol.getInstallPath());
        hmsg.setPrimaryStorageUuid(self.getUuid());
        if (vol.getRootImageUuid() != null) {
            String installUrl = getImageCacheInstallPath(vol.getRootImageUuid());
            if (installUrl != null) {
                hmsg.getExcludeInstallPaths().add(installUrl);
            }
        }
        bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, huuid);
        bus.send(hmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply ret) {
                if (!ret.isSuccess()) {
                    reply.setError(ret.getError());
                }

                bus.reply(msg, reply);
            }
        });
    }

    private String getImageCacheInstallPath(String imageUuid) {
        return Q.New(ImageCacheVO.class)
                .select(ImageCacheVO_.installUrl)
                .eq(ImageCacheVO_.primaryStorageUuid, self.getUuid())
                .eq(ImageCacheVO_.imageUuid, imageUuid).findValue();
    }

    private void handle(final TakeSnapshotMsg msg) {
        final TakeSnapshotReply reply = new TakeSnapshotReply();

        String volumeUuid = msg.getStruct().getCurrent().getVolumeUuid();
        VolumeVO vol = dbf.findByUuid(volumeUuid, VolumeVO.class);

        String huuid;
        String connectedHostUuid = factory.getConnectedHostForOperation(getSelfInventory()).get(0).getUuid();
        if (vol.getVmInstanceUuid() != null) {
            Tuple t = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.state, VmInstanceVO_.hostUuid)
                    .eq(VmInstanceVO_.uuid, vol.getVmInstanceUuid())
                    .findTuple();
            VmInstanceState state = t.get(0, VmInstanceState.class);
            String vmHostUuid = t.get(1, String.class);

            if (state == VmInstanceState.Running || state == VmInstanceState.Paused) {
                DebugUtils.Assert(vmHostUuid != null,
                        String.format("vm[uuid:%s] is Running or Paused, but has no hostUuid", vol.getVmInstanceUuid()));
                huuid = vmHostUuid;
            } else if (state == VmInstanceState.Stopped) {
                huuid = connectedHostUuid;
            } else {
                reply.setError(operr("vm[uuid:%s] is not Running, Paused or Stopped, current state is %s",
                        vol.getVmInstanceUuid(), state));
                bus.reply(msg, reply);
                return;
            }
        } else {
            huuid = connectedHostUuid;
        }

        VolumeInventory volInv = VolumeInventory.valueOf(vol);
        TakeSnapshotOnHypervisorMsg hmsg = new TakeSnapshotOnHypervisorMsg();
        hmsg.setHostUuid(huuid);
        hmsg.setVmUuid(vol.getVmInstanceUuid());
        hmsg.setVolume(volInv);
        hmsg.setSnapshotName(msg.getStruct().getCurrent().getUuid());
        hmsg.setFullSnapshot(msg.getStruct().isFullSnapshot());
        final String installPath = NfsPrimaryStorageKvmHelper.makeKvmSnapshotInstallPath(getSelfInventory(), volInv, msg.getStruct().getCurrent());
        hmsg.setInstallPath(installPath);
        bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, huuid);
        bus.send(hmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply ret) {
                if (ret.isSuccess()) {
                    TakeSnapshotOnHypervisorReply treply = (TakeSnapshotOnHypervisorReply) ret;
                    VolumeSnapshotInventory inv = msg.getStruct().getCurrent();
                    inv.setSize(treply.getSize());
                    inv.setPrimaryStorageUuid(self.getUuid());
                    inv.setPrimaryStorageInstallPath(treply.getSnapshotInstallPath());
                    inv.setType(VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString());
                    reply.setNewVolumeInstallPath(treply.getNewVolumeInstallPath());
                    reply.setInventory(inv);
                } else {
                    reply.setError(ret.getError());
                }

                bus.reply(msg, reply);
            }
        });
    }

    private void handle(PrimaryStorageRemoveCachedImageMsg msg) {
        if (self.getAttachedClusterRefs().isEmpty()) {
            PrimaryStorageRemoveCachedImageReply reply = new PrimaryStorageRemoveCachedImageReply();
            reply.setError(operr("primary storage[uuid:%s] doesn't attach to any cluster", self.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        PrimaryStorageClusterRefVO ref = self.getAttachedClusterRefs().iterator().next();
        ClusterVO cluster = dbf.findByUuid(ref.getClusterUuid(), ClusterVO.class);
        getBackend(HypervisorType.valueOf(cluster.getHypervisorType())).deleteImageCache(msg.getInventory());
    }


    @Transactional(readOnly = true)
    protected NfsPrimaryStorageBackend getUsableBackend() {
        List<String> cuuids = transformAndRemoveNull(self.getAttachedClusterRefs(), PrimaryStorageClusterRefVO::getClusterUuid);

        if (cuuids.isEmpty()) {
            return null;
        }

        String sql = "select cluster.uuid from ClusterVO cluster, HostVO host where cluster.uuid = host.clusterUuid and host.status = :hostStatus and cluster.uuid in (:cuuids)";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("hostStatus", HostStatus.Connected);
        q.setParameter("cuuids", cuuids);
        cuuids = q.getResultList();

        if (cuuids.isEmpty()) {
            return null;
        }

        return getBackendByClusterUuid(cuuids.get(0));
    }

    private NfsPrimaryStorageBackend getBackendByClusterUuid(String clusterUuid) {
        SimpleQuery<ClusterVO> query = dbf.createQuery(ClusterVO.class);
        query.select(ClusterVO_.hypervisorType);
        query.add(ClusterVO_.uuid, Op.EQ, clusterUuid);
        String hvType = query.findValue();
        return getBackend(HypervisorType.valueOf(hvType));
    }

    private NfsPrimaryStorageBackend getBackendByHostUuid(String hostUuid) {
        String hvType = Q.New(HostVO.class).select(HostVO_.hypervisorType)
                .eq(HostVO_.uuid, hostUuid)
                .findValue();
        return getBackend(HypervisorType.valueOf(hvType));
    }

    private NfsPrimaryStorageBackend getBackend(HypervisorType hvType) {
        return factory.getHypervisorBackend(hvType);
    }

    @Override
    public void attachHook(String clusterUuid, Completion completion) {
        NfsPrimaryStorageBackend backend = getBackendByClusterUuid(clusterUuid);
        backend.attachToCluster(PrimaryStorageInventory.valueOf(self), clusterUuid, new ReturnValueCompletion<Boolean>(completion) {
            @Override
            public void success(Boolean ret) {
                if (ret) {
                    changeStatus(PrimaryStorageStatus.Connected);
                }
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }


    @Override
    public void detachHook(String clusterUuid, Completion completion) {
        NfsPrimaryStorageBackend backend = getBackendByClusterUuid(clusterUuid);
        try {
            backend.detachFromCluster(PrimaryStorageInventory.valueOf(self), clusterUuid);
            completion.success();
        } catch (NfsPrimaryStorageException e) {
            completion.fail(operr(e.getMessage()));
        }
    }

    private void handle(final InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        final VolumeInventory volume = msg.getVolume();
        volume.setInstallPath(NfsPrimaryStorageKvmHelper.makeTemporaryRootVolumeInstallUrl(
                getSelfInventory(), volume, msg.getOriginVolumeUuid()));
        handle((InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) msg);
    }

    private void handle(final InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        final InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
        final ImageSpec ispec = msg.getTemplateSpec();
        final VolumeInventory volume = msg.getVolume();
        final ImageInventory image = ispec.getInventory();

        if (ImageMediaType.RootVolumeTemplate.toString().equals(image.getMediaType())) {
            FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("create-root-volume-from-image-%s", image.getUuid()));
            chain.then(new ShareFlow() {
                PrimaryStorageInventory primaryStorage = getSelfInventory();
                ImageCacheInventory imageCache;
                String volumeInstallPath;
                Long actualSize;

                @Override
                public void setup() {
                    flow(new NoRollbackFlow() {
                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            NfsDownloadImageToCacheJob job = new NfsDownloadImageToCacheJob();
                            job.setImage(msg.getTemplateSpec());
                            job.setPrimaryStorage(primaryStorage);

                            jobf.execute(NfsPrimaryStorageKvmHelper.makeDownloadImageJobName(image, primaryStorage),
                                    NfsPrimaryStorageKvmHelper.makeJobOwnerName(primaryStorage), job,
                                    new ReturnValueCompletion<ImageCacheInventory>(trigger) {
                                        @Override
                                        public void success(ImageCacheInventory returnValue) {
                                            imageCache = returnValue;
                                            trigger.next();
                                        }

                                        @Override
                                        public void fail(ErrorCode errorCode) {
                                            trigger.fail(errorCode);
                                        }
                                    }, ImageCacheInventory.class);
                        }
                    });

                    flow(new NoRollbackFlow() {
                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            NfsPrimaryStorageBackend backend = factory.getHypervisorBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(image.getFormat(), self.getUuid()));
                            backend.createVolumeFromImageCache(primaryStorage, image, imageCache, volume, new ReturnValueCompletion<VolumeStats>(trigger) {
                                @Override
                                public void success(VolumeStats returnValue) {
                                    volumeInstallPath = returnValue.getInstallPath();
                                    actualSize = returnValue.getActualSize();
                                    if (returnValue.getSize() != null) {
                                        volume.setSize(returnValue.getSize());
                                    }

                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });

                    done(new FlowDoneHandler(msg) {
                        @Override
                        public void handle(Map data) {
                            volume.setInstallPath(volumeInstallPath);
                            volume.setFormat(image.getFormat());
                            volume.setActualSize(actualSize);
                            reply.setVolume(volume);
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
        } else {
            createEmptyVolume(msg);
        }
    }

    private void createMemoryVolume(InstantiateMemoryVolumeOnPrimaryStorageMsg msg) {
        NfsPrimaryStorageBackend backend;
        if (msg.getDestHost() != null) {
            backend = getBackend(HypervisorType.valueOf(msg.getDestHost().getHypervisorType()));
        } else {
            backend = getUsableBackend();
            if (backend == null) {
                throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find any usable host to" +
                                " create the data volume[uuid:%s, name:%s]", self.getUuid(), self.getName(),
                        msg.getVolume().getUuid(), msg.getVolume().getName()));
            }
        }

        VolumeInventory vol = msg.getVolume();
        final InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
        backend.createMemoryVolume(PrimaryStorageInventory.valueOf(self), msg.getVolume(), new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String installUrl) {
                vol.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                vol.setInstallPath(installUrl);
                reply.setVolume(vol);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void createTemporaryEmptyVolume(final InstantiateTemporaryVolumeOnPrimaryStorageMsg msg) {
        VolumeInventory volume = msg.getVolume();
        if (VolumeType.Root.toString().equals(volume.getType())) {
            volume.setInstallPath(NfsPrimaryStorageKvmHelper
                    .makeTemporaryRootVolumeInstallUrl(getSelfInventory(), volume, msg.getOriginVolumeUuid()));
        } else {
            volume.setInstallPath(NfsPrimaryStorageKvmHelper
                    .makeTemporaryDataVolumeInstallUrl(getSelfInventory(), volume.getUuid(), msg.getOriginVolumeUuid()));
        }
        createEmptyVolume(msg);
    }

    private void createEmptyVolume(final InstantiateVolumeOnPrimaryStorageMsg msg) {
        NfsPrimaryStorageBackend backend;
        if (msg.getDestHost() != null) {
            backend = getBackend(HypervisorType.valueOf(msg.getDestHost().getHypervisorType()));
        } else {
            backend = getUsableBackend();
            if (backend == null) {
                throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find any usable host to" +
                                " create the data volume[uuid:%s, name:%s]", self.getUuid(), self.getName(),
                        msg.getVolume().getUuid(), msg.getVolume().getName()));
            }
        }

        VolumeInventory vol = msg.getVolume();
        final InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
        backend.instantiateVolume(PrimaryStorageInventory.valueOf(self), msg.getDestHost(), vol, new ReturnValueCompletion<VolumeInventory>(msg) {
            @Override
            public void success(VolumeInventory returnValue) {
                reply.setVolume(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                reply.setSuccess(false);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(InstantiateVolumeOnPrimaryStorageMsg msg) {
        if (msg instanceof InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg) {
            handle((InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg) msg);
        } else if (msg instanceof InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) {
            handle((InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) msg);
        } else if (msg instanceof InstantiateTemporaryVolumeOnPrimaryStorageMsg) {
            createTemporaryEmptyVolume((InstantiateTemporaryVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof InstantiateMemoryVolumeOnPrimaryStorageMsg) {
            createMemoryVolume((InstantiateMemoryVolumeOnPrimaryStorageMsg) msg);
        } else {
            createEmptyVolume(msg);
        }
    }

    @Override
    protected void handle(final DeleteVolumeOnPrimaryStorageMsg msg) {
        final DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
        final VolumeInventory vol = msg.getVolume();
        final NfsPrimaryStorageBackend backend = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(vol.getFormat(), self.getUuid()));

        Completion completion = new Completion(msg) {
            @Override
            public void success() {
                logger.debug(String.format("successfully delete volume[uuid:%s, installPath:%s] on nfs primary storage[uuid:%s]", vol.getUuid(),
                        vol.getInstallPath(), self.getUuid()));
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (errorCode.isError(VolumeErrors.VOLUME_IN_USE)) {
                    logger.debug(String.format("unable to delete volume[uuid:%s] right now, skip this GC job because it's in use", msg.getVolume().getUuid()));
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                    return;
                }
                NfsDeleteVolumeGC gc = new NfsDeleteVolumeGC();
                gc.NAME = String.format("gc-nfs-%s-volume-%s", self.getUuid(), vol.getUuid());
                gc.primaryStorageUuid = self.getUuid();
                gc.hypervisorType = backend.getHypervisorType().toString();
                gc.volume = vol;
                gc.submit(NfsPrimaryStorageGlobalConfig.GC_INTERVAL.value(Long.class), TimeUnit.SECONDS);

                bus.reply(msg, reply);
            }
        };

        if (vol.getType().equals(VolumeType.Memory.toString())) {
            backend.deleteFolder(getSelfInventory(), vol.getInstallPath(), completion);
        } else {
            backend.delete(getSelfInventory(), vol.getInstallPath(), completion);
        }
    }

    @Override
    protected void handle(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg) {
        CreateImageCacheFromVolumeOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeOnPrimaryStorageReply();

        ImageSpec spec = new ImageSpec();
        spec.setInventory(msg.getImageInventory());

        NfsDownloadImageToCacheJob job = new NfsDownloadImageToCacheJob();
        job.setPrimaryStorage(getSelfInventory());
        job.setImage(spec);
        job.setVolumeResourceInstallPath(msg.getVolumeInventory().getInstallPath());

        jobf.execute(NfsPrimaryStorageKvmHelper.makeDownloadImageJobName(msg.getImageInventory(), job.getPrimaryStorage()),
                NfsPrimaryStorageKvmHelper.makeJobOwnerName(job.getPrimaryStorage()), job,
                new ReturnValueCompletion<ImageCacheInventory>(msg) {

                    @Override
                    public void success(ImageCacheInventory cache) {
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                }, ImageCacheInventory.class);
    }

    @Override
    protected void handle(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply();

        boolean useSnapshotAsCache = msg.hasSystemTag(VolumeSystemTags.FAST_CREATE.getTagFormat());
        if (useSnapshotAsCache) {
            ImageCacheVO cache = createTemporaryImageCacheFromVolumeSnapshot(msg.getImageInventory(), msg.getVolumeSnapshot());
            dbf.persist(cache);
            reply.setInventory(cache.toInventory());
            reply.setIncremental(true);
            bus.reply(msg, reply);
            return;
        }

        ImageSpec spec = new ImageSpec();
        spec.setInventory(msg.getImageInventory());

        NfsDownloadImageToCacheJob job = new NfsDownloadImageToCacheJob();
        job.setPrimaryStorage(getSelfInventory());
        job.setImage(spec);
        job.setVolumeResourceInstallPath(msg.getVolumeSnapshot().getPrimaryStorageInstallPath());

        jobf.execute(NfsPrimaryStorageKvmHelper.makeDownloadImageJobName(msg.getImageInventory(), job.getPrimaryStorage()),
                NfsPrimaryStorageKvmHelper.makeJobOwnerName(job.getPrimaryStorage()), job,
                new ReturnValueCompletion<ImageCacheInventory>(msg) {

                    @Override
                    public void success(ImageCacheInventory cache) {
                        reply.setInventory(cache);
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                }, ImageCacheInventory.class);
    }

    @Override
    protected void handle(final CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        final CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();
        final VolumeInventory volume = msg.getVolumeInventory();
        BackupStorageVO bsvo = dbf.findByUuid(msg.getBackupStorageUuid(), BackupStorageVO.class);
        final BackupStorageInventory bsinv = BackupStorageInventory.valueOf(bsvo);
        final PrimaryStorageInventory pinv = getSelfInventory();
        final ImageInventory image = msg.getImageInventory();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-template-from-volume-%s-on-nfs-primary-storage-%s", volume.getUuid(), self.getUuid()));
        chain.enableProgressReport();
        chain.then(new ShareFlow() {
            String templatePrimaryStorageInstallPath;
            String templateBackupStorageInstallPath;

            @Override
            public void setup() {
                flow(new Flow() {
                    NfsPrimaryStorageBackend bkd = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(volume.getFormat(), self.getUuid()));

                    @Override
                    public String name() {
                        return "create-template-from-the-specific-volume";
                    }

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        bkd.createTemplateFromVolume(pinv, volume, image, new ReturnValueCompletion<NfsPrimaryStorageBackend.BitsInfo>(trigger) {
                            @Override
                            public void success(NfsPrimaryStorageBackend.BitsInfo info) {
                                reply.setActualSize(info.getActualSize());
                                templatePrimaryStorageInstallPath = info.getInstallPath();
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (templatePrimaryStorageInstallPath != null) {
                            bkd.delete(pinv, templatePrimaryStorageInstallPath, new NopeCompletion());
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public String name() {
                        return "upload-template";
                    }

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        NfsPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(
                                BackupStorageType.valueOf(bsinv.getType()),
                                nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(volume.getFormat(), self.getUuid())
                        );

                        DebugUtils.Assert(!ImageMediaType.ISO.toString().equals(image.getMediaType()), String.format("how can this happen? creating an template from an ISO????"));
                        templateBackupStorageInstallPath = ImageMediaType.RootVolumeTemplate.toString().equals(image.getMediaType()) ?
                                mediator.makeRootVolumeTemplateInstallPath(bsinv.getUuid(), image.getUuid()) : mediator.makeDataVolumeTemplateInstallPath(bsinv.getUuid(), image.getUuid());
                        mediator.uploadBits(msg.getImageInventory().getUuid(), pinv, bsinv, templateBackupStorageInstallPath, templatePrimaryStorageInstallPath, new ReturnValueCompletion<String>(trigger) {
                            @Override
                            public void success(String installPath) {
                                templateBackupStorageInstallPath = installPath;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        reply.setTemplateBackupStorageInstallPath(templateBackupStorageInstallPath);
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

    @Override
    protected void handle(final DownloadDataVolumeToPrimaryStorageMsg msg) {
        final DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();
        String installPath;
        if (msg instanceof DownloadTemporaryDataVolumeToPrimaryStorageMsg) {
            String originVolumeUuid = ((DownloadTemporaryDataVolumeToPrimaryStorageMsg) msg).getOriginVolumeUuid();
            installPath = NfsPrimaryStorageKvmHelper.makeTemporaryDataVolumeInstallUrl(getSelfInventory(), msg.getVolumeUuid(), originVolumeUuid);
        } else {
            installPath = NfsPrimaryStorageKvmHelper.makeDataVolumeInstallUrl(getSelfInventory(), msg.getVolumeUuid());
        }

        BackupStorageVO bsvo = dbf.findByUuid(msg.getBackupStorageRef().getBackupStorageUuid(), BackupStorageVO.class);
        NfsPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(
                BackupStorageType.valueOf(bsvo.getType()),
                nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(msg.getImage().getFormat(), self.getUuid())
        );

        mediator.downloadBits(getSelfInventory(), BackupStorageInventory.valueOf(bsvo),
                msg.getBackupStorageRef().getInstallPath(), installPath, true, new Completion(msg) {
                    @Override
                    public void success() {
                        reply.setInstallPath(installPath);
                        saveVolumeProvisioningStrategy(msg.getVolumeUuid(), VolumeProvisioningStrategy.ThinProvisioning);
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });
    }

    @Override
    protected void handle(GetInstallPathForDataVolumeDownloadMsg msg) {
        final GetInstallPathForDataVolumeDownloadReply reply = new GetInstallPathForDataVolumeDownloadReply();
        if (msg instanceof GetInstallPathForTemporaryDataVolumeDownloadMsg) {
            String originVolumeUuid = ((GetInstallPathForTemporaryDataVolumeDownloadMsg) msg).getOriginVolumeUuid();
            reply.setInstallPath(NfsPrimaryStorageKvmHelper.makeTemporaryDataVolumeInstallUrl(getSelfInventory(), msg.getVolumeUuid(), originVolumeUuid));
        } else {
            reply.setInstallPath(NfsPrimaryStorageKvmHelper.makeDataVolumeInstallUrl(getSelfInventory(), msg.getVolumeUuid()));
        }
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(final DeleteVolumeBitsOnPrimaryStorageMsg msg) {
        final DeleteVolumeBitsOnPrimaryStorageReply reply = new DeleteVolumeBitsOnPrimaryStorageReply();
        NfsPrimaryStorageBackend bkd = getBackend(HypervisorType.valueOf(msg.getHypervisorType()));
        if (msg.isFolder()) {
            bkd.deleteFolder(getSelfInventory(), msg.getInstallPath(), new Completion(msg) {
                @Override
                public void success() {
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        } else {
            bkd.delete(getSelfInventory(), msg.getInstallPath(), new Completion(msg) {
                @Override
                public void success() {
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        }
    }

    @Override
    protected void handle(final DeleteBitsOnPrimaryStorageMsg msg) {
        final DeleteVolumeBitsOnPrimaryStorageReply reply = new DeleteVolumeBitsOnPrimaryStorageReply();
        String hostUuid = getAvailableHostUuidForOperation();
        if (hostUuid == null) {
            bus.reply(msg, reply);
            return;
        }
        String type = Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).select(HostVO_.hypervisorType).findValue();
        NfsPrimaryStorageBackend bkd = getBackend(HypervisorType.valueOf(type));
        if (msg.isFolder()) {
            bkd.deleteFolder(getSelfInventory(), msg.getInstallPath(), new Completion(msg) {
                @Override
                public void success() {
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        } else {
            bkd.delete(getSelfInventory(), msg.getInstallPath(), new Completion(msg) {
                @Override
                public void success() {
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        }
    }

    @Override
    protected void handle(final DownloadIsoToPrimaryStorageMsg msg) {
        final DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
        final PrimaryStorageInventory pinv = getSelfInventory();
        NfsDownloadImageToCacheJob job = new NfsDownloadImageToCacheJob();
        job.setPrimaryStorage(pinv);
        job.setImage(msg.getIsoSpec());

        final ImageInventory img = msg.getIsoSpec().getInventory();
        jobf.execute(NfsPrimaryStorageKvmHelper.makeDownloadImageJobName(msg.getIsoSpec().getInventory(), pinv),
                NfsPrimaryStorageKvmHelper.makeJobOwnerName(pinv), job,
                new ReturnValueCompletion<ImageCacheInventory>(msg) {

                    @Override
                    public void success(ImageCacheInventory returnValue) {
                        logger.debug(String.format("successfully downloaded iso[uuid:%s, name:%s] from backup storage[uuid:%s] to primary storage[uuid:%s, name:%s], path in cache: %s",
                                img.getUuid(), img.getName(), msg.getIsoSpec().getSelectedBackupStorage().getBackupStorageUuid(),
                                pinv.getUuid(), pinv.getName(), returnValue.getInstallUrl()));

                        reply.setInstallPath(returnValue.getInstallUrl());
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        String err = String.format("failed to downloaded iso[uuid:%s, name:%s] from backup storage[uuid:%s] to primary storage[uuid:%s, name:%s]",
                                img.getUuid(), img.getName(), msg.getIsoSpec().getSelectedBackupStorage().getBackupStorageUuid(),
                                pinv.getUuid(), pinv.getName());
                        logger.warn(err);
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                }, ImageCacheInventory.class);

    }

    @Override
    protected void handle(DeleteIsoFromPrimaryStorageMsg msg) {
        DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {
        VolumeSnapshotCapability capability = new VolumeSnapshotCapability();
        HypervisorType hvType = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getVolume().getFormat());
        if (hvType.toString().equals(KVMConstant.KVM_HYPERVISOR_TYPE)) {
            String volumeType = msg.getVolume().getType();
            if (VolumeType.Data.toString().equals(volumeType) || VolumeType.Root.toString().equals(volumeType)) {
                capability.setArrangementType(VolumeSnapshotArrangementType.CHAIN);
            } else if (VolumeType.Memory.toString().equals(volumeType)) {
                capability.setArrangementType(VolumeSnapshotArrangementType.INDIVIDUAL);
            } else {
                throw new CloudRuntimeException(String.format("unknown volume type %s", volumeType));
            }
            capability.setSupport(true);
        } else {
            capability.setSupport(false);
        }

        AskVolumeSnapshotCapabilityReply reply = new AskVolumeSnapshotCapabilityReply();
        reply.setCapability(capability);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(final SyncVolumeSizeOnPrimaryStorageMsg msg) {
        NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find hosts in attached clusters to perform the operation",
                    self.getUuid(), self.getName()));
        }

        backend.handle(getSelfInventory(), msg, new ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(SyncVolumeSizeOnPrimaryStorageReply reply) {
                saveVolumeProvisioningStrategy(msg.getVolumeUuid(), reply.getActualSize() < reply.getSize() ? VolumeProvisioningStrategy.ThinProvisioning : VolumeProvisioningStrategy.ThickProvisioning);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(EstimateVolumeTemplateSizeOnPrimaryStorageMsg msg) {
        NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find hosts in attached clusters to perform the operation",
                    self.getUuid(), self.getName()));
        }

        backend.handle(getSelfInventory(), msg, new ReturnValueCompletion<EstimateVolumeTemplateSizeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(EstimateVolumeTemplateSizeOnPrimaryStorageReply reply) {
                saveVolumeProvisioningStrategy(msg.getVolumeUuid(), reply.getActualSize() < reply.getSize() ? VolumeProvisioningStrategy.ThinProvisioning : VolumeProvisioningStrategy.ThickProvisioning);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                EstimateVolumeTemplateSizeOnPrimaryStorageReply reply = new EstimateVolumeTemplateSizeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(BatchSyncVolumeSizeOnPrimaryStorageMsg msg) {
        NfsPrimaryStorageBackend backend = getBackendByHostUuid(msg.getHostUuid());
        backend.handle(getSelfInventory(), msg, new ReturnValueCompletion<BatchSyncVolumeSizeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(BatchSyncVolumeSizeOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }
            @Override
            public void fail(ErrorCode errorCode) {
                BatchSyncVolumeSizeOnPrimaryStorageReply reply = new BatchSyncVolumeSizeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected void saveVolumeProvisioningStrategy(String volumeUuid, VolumeProvisioningStrategy strategy) {
        if (!VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.hasTag(volumeUuid)) {
            SystemTagCreator tagCreator = VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.newSystemTagCreator(volumeUuid);
            tagCreator.setTagByTokens(
                    map(e(VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN, strategy))
            );
            tagCreator.inherent = false;
            tagCreator.create();
        }
    }

    private void handle(NfsToNfsMigrateBitsMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                NfsPrimaryStorageBackend backend = getUsableBackend();
                if (backend == null) {
                    throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find hosts in attached clusters to perform the operation",
                            self.getUuid(), self.getName()));
                }

                backend.handle(getSelfInventory(), msg, new ReturnValueCompletion<NfsToNfsMigrateBitsReply>(msg) {
                    @Override
                    public void success(NfsToNfsMigrateBitsReply reply) {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        NfsToNfsMigrateBitsReply reply = new NfsToNfsMigrateBitsReply();
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("migrate-bits-from-host-%s-to-ps-%s", msg.getHostUuid(), msg.getPrimaryStorageUuid());
            }
        });
    }

    private void handle(DownloadBitsFromKVMHostToPrimaryStorageMsg msg) {
        NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find hosts in attached clusters to perform the operation",
                    self.getUuid(), self.getName()));
        }

        backend.handle(getSelfInventory(), msg, new ReturnValueCompletion<DownloadBitsFromKVMHostToPrimaryStorageReply>(msg) {
            @Override
            public void success(DownloadBitsFromKVMHostToPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DownloadBitsFromKVMHostToPrimaryStorageReply reply = new DownloadBitsFromKVMHostToPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(CancelDownloadBitsFromKVMHostToPrimaryStorageMsg msg) {
        NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find hosts in attached clusters to perform the operation",
                    self.getUuid(), self.getName()));
        }

        backend.handle(getSelfInventory(), msg, new ReturnValueCompletion<CancelDownloadBitsFromKVMHostToPrimaryStorageReply>(msg) {
            @Override
            public void success(CancelDownloadBitsFromKVMHostToPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CancelDownloadBitsFromKVMHostToPrimaryStorageReply reply = new CancelDownloadBitsFromKVMHostToPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(GetDownloadBitsFromKVMHostProgressMsg msg) {
        NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find hosts in attached clusters to perform the operation",
                    self.getUuid(), self.getName()));
        }

        backend.handle(getSelfInventory(), msg, new ReturnValueCompletion<GetDownloadBitsFromKVMHostProgressReply>(msg) {
            @Override
            public void success(GetDownloadBitsFromKVMHostProgressReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                GetDownloadBitsFromKVMHostProgressReply reply = new GetDownloadBitsFromKVMHostProgressReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(NfsRebaseVolumeBackingFileMsg msg) {
        NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find hosts in attached clusters to perform the operation",
                    self.getUuid(), self.getName()));
        }
        backend.handle(getSelfInventory(), msg, new ReturnValueCompletion<NfsRebaseVolumeBackingFileReply>(msg) {
            @Override
            public void success(NfsRebaseVolumeBackingFileReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                NfsRebaseVolumeBackingFileReply reply = new NfsRebaseVolumeBackingFileReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(GetVolumeBackingChainFromPrimaryStorageMsg msg) {
        NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find hosts in attached clusters to perform the operation",
                    self.getUuid(), self.getName()));
        }
        backend.handle(getSelfInventory(), msg, new ReturnValueCompletion<GetVolumeBackingChainFromPrimaryStorageReply>(msg) {
            @Override
            public void success(GetVolumeBackingChainFromPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                GetVolumeBackingChainFromPrimaryStorageReply reply = new GetVolumeBackingChainFromPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(NfsRecalculatePrimaryStorageCapacityMsg msg) {
        if (msg.isRelease()) {
            doReleasePrimaryStorageCapacity();
        } else {
            RecalculatePrimaryStorageCapacityMsg rmsg = new RecalculatePrimaryStorageCapacityMsg();
            rmsg.setPrimaryStorageUuid(self.getUuid());
            bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
            bus.send(rmsg);
        }
    }

    private void doReleasePrimaryStorageCapacity() {
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(self.getUuid());
        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                cap.setAvailableCapacity(0L);
                cap.setAvailablePhysicalCapacity(0L);
                cap.setSystemUsedCapacity(0L);
                cap.setTotalPhysicalCapacity(0L);
                cap.setTotalCapacity(0L);
                return cap;
            }
        });
    }

    @Override
    protected void connectHook(ConnectParam param, final Completion completion) {
        final NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            // the nfs primary storage has not been attached to any clusters, or no connected hosts
            completion.fail(err(PrimaryStorageErrors.DISCONNECTED,
                    "the NFS primary storage[uuid:%s, name:%s] has not attached to any clusters, or no hosts in the" +
                            " attached clusters are connected", self.getUuid(), self.getName()
            ));

            return;
        }

        logger.debug(String.format("reconnect-nfs-primary-storage-%s", self.getUuid()));
        SimpleQuery<PrimaryStorageClusterRefVO> q = dbf.createQuery(PrimaryStorageClusterRefVO.class);
        q.select(PrimaryStorageClusterRefVO_.clusterUuid);
        q.add(PrimaryStorageClusterRefVO_.primaryStorageUuid, Op.EQ, self.getUuid());
        List<String> cuuids = q.listValue();
        if (cuuids.isEmpty()) {
            completion.success();
            return;
        }

        PrimaryStorageInventory inv = getSelfInventory();
        new LoopAsyncBatch<String>(completion) {
            boolean success;

            @Override
            protected Collection<String> collect() {
                return cuuids;
            }

            @Override
            protected AsyncBatchRunner forEach(String cuuid) {
                return new AsyncBatchRunner() {
                    @Override
                    public void run(NoErrorCompletion completion) {
                        NfsPrimaryStorageBackend bkd = getBackendByClusterUuid(cuuid);
                        bkd.remount(inv, cuuid, new Completion(completion) {
                            @Override
                            public void success() {
                                success = true;
                                completion.done();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                errors.add(errorCode);
                                completion.done();
                            }
                        });
                    }
                };
            }

            @Override
            protected void done() {
                if (success) {
                    self = dbf.reload(self);
                    completion.success();
                } else {
                    completion.fail(errf.stringToOperationError(String.format("unable to connect the" +
                            "NFS primary storage[uuid:%s, name:%s]", self.getUuid(), self.getName()), errors));
                }
            }
        }.start();
    }

    @Override
    protected void pingHook(Completion completion) {
        NfsPrimaryStorageBackend bkd = getUsableBackend();
        if (bkd == null) {
            // the nfs primary storage has not been attached to any clusters, or no connected hosts
            completion.fail(operr("the NFS primary storage[uuid:%s, name:%s] has not attached to any clusters, or no hosts in the" +
                    " attached clusters are connected", self.getUuid(), self.getName()));
        } else {
            bkd.ping(getSelfInventory(), completion);
        }
    }

    @Override
    protected void handle(ShrinkVolumeSnapshotOnPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    protected void handle(GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg) {
        NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find hosts in attached clusters to perform the operation",
                    self.getUuid(), self.getName()));
        }

        backend.handle(getSelfInventory(), msg, new ReturnValueCompletion<GetVolumeSnapshotEncryptedOnPrimaryStorageReply>(msg) {
            @Override
            public void success(GetVolumeSnapshotEncryptedOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                GetVolumeSnapshotEncryptedOnPrimaryStorageReply reply = new GetVolumeSnapshotEncryptedOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            PhysicalCapacityUsage usage = new PhysicalCapacityUsage();
            usage.availablePhysicalSize = 0;
            usage.totalPhysicalSize = 0;
            completion.success(usage);
        } else {
            backend.getPhysicalCapacity(getSelfInventory(), completion);
        }
    }

    private String getAvailableHostUuidForOperation() {
        List<String> hostUuids = Q.New(PrimaryStorageHostRefVO.class).
                eq(PrimaryStorageHostRefVO_.primaryStorageUuid, self.getUuid()).select(PrimaryStorageHostRefVO_.hostUuid).listValues();
        if (hostUuids == null || hostUuids.size() == 0) {
            return null;
        }
        return hostUuids.get(0);
    }

    @Override
    public void handle(AskInstallPathForNewSnapshotMsg msg) {
        NfsPrimaryStorageBackend bkd = getUsableBackend();
        if (bkd == null) {
            throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find hosts in attached clusters to perform the operation",
                    self.getUuid(), self.getName()));
        }

        bkd.handle(getSelfInventory(), msg, new ReturnValueCompletion<AskInstallPathForNewSnapshotReply>(msg) {
            @Override
            public void success(AskInstallPathForNewSnapshotReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                AskInstallPathForNewSnapshotReply reply = new AskInstallPathForNewSnapshotReply();
                reply.setSuccess(false);
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(GetPrimaryStorageResourceLocationMsg msg) {
        bus.reply(msg, new GetPrimaryStorageResourceLocationReply());
    }

    @Override
    protected void handle(CheckVolumeSnapshotOperationOnPrimaryStorageMsg msg) {
        CheckVolumeSnapshotOperationOnPrimaryStorageReply reply = new CheckVolumeSnapshotOperationOnPrimaryStorageReply();
        if (msg.getVmInstanceUuid() != null) {
            HostStatus hostStatus = SQL.New("select host.status from VmInstanceVO vm, HostVO host" +
                    " where vm.uuid = :vmUuid" +
                    " and vm.hostUuid = host.uuid", HostStatus.class)
                    .param("vmUuid", msg.getVmInstanceUuid())
                    .find();
            if (hostStatus == null && getAvailableHostUuidForOperation() == null) {
                reply.setError(err(HostErrors.HOST_IS_DISCONNECTED, "cannot find available host for operation on" +
                        " primary storage[uuid:%s].", self.getUuid()));
            } else if (hostStatus != HostStatus.Connected && hostStatus != null) {
                reply.setError(err(HostErrors.HOST_IS_DISCONNECTED, "host where vm[uuid:%s] locate is not Connected.", msg.getVmInstanceUuid()));
            }
        }

        bus.reply(msg, reply);
    }

    private ErrorCode checkChangeVolumeType(String volumeUuid) {
        List<VolumeInventory> refVols = VolumeSnapshotReferenceUtils.getReferenceVolume(volumeUuid);
        if (refVols.isEmpty()) {
            return null;
        }

        List<String> infos = refVols.stream().map(v -> String.format("uuid:%s, name:%s", v.getUuid(), v.getName())).collect(Collectors.toList());
        return operr("volume[uuid:%s] has reference volume[%s], can not change volume type before flatten " +
                "them and their descendants", volumeUuid, infos.toString());
    }

    @Override
    protected void handle(CheckChangeVolumeTypeOnPrimaryStorageMsg msg) {
        CheckChangeVolumeTypeOnPrimaryStorageReply reply = new CheckChangeVolumeTypeOnPrimaryStorageReply();
        ErrorCode errorCode = checkChangeVolumeType(msg.getVolume().getUuid());
        if (errorCode != null) {
            reply.setError(errorCode);;
        }

        bus.reply(msg, reply);
    }

    @Override
    protected void handle(ChangeVolumeTypeOnPrimaryStorageMsg msg) {
        ErrorCode errorCode = checkChangeVolumeType(msg.getVolume().getUuid());
        if (errorCode != null) {
            ChangeVolumeTypeOnPrimaryStorageReply reply = new ChangeVolumeTypeOnPrimaryStorageReply();
            reply.setError(errorCode);
            bus.reply(msg, reply);
            return;
        }

        NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find hosts in attached clusters to perform the operation",
                    self.getUuid(), self.getName()));
        }

        backend.handle(getSelfInventory(), msg, new ReturnValueCompletion<ChangeVolumeTypeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(ChangeVolumeTypeOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                ChangeVolumeTypeOnPrimaryStorageReply r = new ChangeVolumeTypeOnPrimaryStorageReply();
                r.setError(errorCode);
                bus.reply(msg, r);
            }
        });
    }

    protected void handle(UnlinkBitsOnPrimaryStorageMsg msg) {
        UnlinkBitsOnPrimaryStorageReply reply = new UnlinkBitsOnPrimaryStorageReply();
        NfsPrimaryStorageBackend backend = getUsableBackend();
        if (backend == null) {
            throw new OperationFailureException(operr("the NFS primary storage[uuid:%s, name:%s] cannot find hosts in attached clusters to perform the operation",
                    self.getUuid(), self.getName()));
        }

        backend.unlink(getSelfInventory(), msg.getInstallPath(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(CommitVolumeSnapshotOnPrimaryStorageMsg msg) {
        CommitVolumeSnapshotOnPrimaryStorageReply reply = new CommitVolumeSnapshotOnPrimaryStorageReply();

        String hostUuid = getHostUuidFromVolume(msg.getVolume().getUuid());
        if (hostUuid == null || hostUuid.isEmpty()) {
            reply.setError(operr("no host found for volume[uuid:%s]", msg.getVolume().getUuid()));
            bus.reply(msg, reply);
            return;
        }

        final NfsPrimaryStorageBackend backend = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(
                msg.getVolume().getFormat(), self.getUuid()));

        backend.commitSnapshot(msg, hostUuid, new ReturnValueCompletion<CommitVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CommitVolumeSnapshotOnPrimaryStorageReply r) {
                bus.reply(msg, r);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(PullVolumeSnapshotOnPrimaryStorageMsg msg) {
        PullVolumeSnapshotOnPrimaryStorageReply reply = new PullVolumeSnapshotOnPrimaryStorageReply();

        String hostUuid = getHostUuidFromVolume(msg.getVolume().getUuid());
        if (hostUuid == null || hostUuid.isEmpty()) {
            reply.setError(operr("no host found for volume[uuid:%s]", msg.getVolume().getUuid()));
            bus.reply(msg, reply);
            return;
        }

        final NfsPrimaryStorageBackend backend = getBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(
                msg.getVolume().getFormat(), self.getUuid()));

        backend.pullSnapshot(msg, hostUuid, new ReturnValueCompletion<PullVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(PullVolumeSnapshotOnPrimaryStorageReply r) {
                bus.reply(msg, r);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private String getHostUuidFromVolume(String volumeUuid) {
        VolumeVO vol = dbf.findByUuid(volumeUuid, VolumeVO.class);
        if (vol == null) {
            return "";
        }

        String hostUuid = "";
        List<HostInventory> connectedHost = factory.getConnectedHostForOperation(getSelfInventory());
        if (connectedHost.isEmpty()) {
            return hostUuid;
        }
        String connectedHostUuid = factory.getConnectedHostForOperation(getSelfInventory()).get(0).getUuid();
        if (vol.getVmInstanceUuid() != null) {
            Tuple t = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.state, VmInstanceVO_.hostUuid)
                    .eq(VmInstanceVO_.uuid, vol.getVmInstanceUuid())
                    .findTuple();
            VmInstanceState state = t.get(0, VmInstanceState.class);
            String vmHostUuid = t.get(1, String.class);

            if (state == VmInstanceState.Running || state == VmInstanceState.Paused) {
                DebugUtils.Assert(vmHostUuid != null,
                        String.format("vm[uuid:%s] is Running or Paused, but has no hostUuid", vol.getVmInstanceUuid()));
                hostUuid = vmHostUuid;
            } else if (state == VmInstanceState.Stopped) {
                hostUuid = connectedHostUuid;
            }
        } else {
            hostUuid = connectedHostUuid;
        }

        return hostUuid;
    }

    @Override
    protected void handle(UpdateVmInstanceMetadataOnPrimaryStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("update-metadata-on-ps-%s", self.getUuid());
            }

            @Override
            public int getSyncLevel() {
                return 10;
            }

            @Override
            public void run(SyncTaskChain chain) {
                UpdateVmInstanceMetadataOnPrimaryStorageReply reply = new UpdateVmInstanceMetadataOnPrimaryStorageReply();

                String hostUuid = getHostUuidFromVolume(msg.getRootVolumeUuid());
                if (hostUuid == null || hostUuid.isEmpty()) {
                    reply.setError(operr("no host found for volume[uuid:%s]", msg.getRootVolumeUuid()));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                final NfsPrimaryStorageBackend backend = getBackendByHostUuid(hostUuid);
                backend.handle(msg, hostUuid, new ReturnValueCompletion<UpdateVmInstanceMetadataOnPrimaryStorageReply>(msg, chain) {
                    @Override
                    public void success(UpdateVmInstanceMetadataOnPrimaryStorageReply r) {
                        bus.reply(msg, r);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("update-metadata-on-ps-%s", self.getUuid());
            }
        });
    }

    @Override
    protected void handle(GetVmInstanceMetadataFromPrimaryStorageMsg msg) {
        GetVmInstanceMetadataFromPrimaryStorageReply reply = new GetVmInstanceMetadataFromPrimaryStorageReply();
        List<HostInventory> connectedHosts = factory.getConnectedHostForOperation(getSelfInventory());
        if (connectedHosts.isEmpty()) {
            reply.setError(operr("no connected host found for NFS primary storage[uuid:%s]", self.getUuid()));
            bus.reply(msg, reply);
            return;
        }
        String hostUuid = msg.getHostUuid();
        String finalHostUuid = hostUuid;
        if (hostUuid == null || !connectedHosts.stream().anyMatch(h -> h.getUuid().equals(finalHostUuid))) {
            hostUuid = connectedHosts.get(0).getUuid();
        }
        final NfsPrimaryStorageBackend backend = getBackendByHostUuid(hostUuid);
        backend.handle(msg, hostUuid, new ReturnValueCompletion<GetVmInstanceMetadataFromPrimaryStorageReply>(msg) {
            @Override
            public void success(GetVmInstanceMetadataFromPrimaryStorageReply r) {
                bus.reply(msg, r);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(ScanVmInstanceMetadataFromPrimaryStorageMsg msg) {
        ScanVmInstanceMetadataFromPrimaryStorageReply reply = new ScanVmInstanceMetadataFromPrimaryStorageReply();
        List<HostInventory> connectedHosts = factory.getConnectedHostForOperation(getSelfInventory());
        if (connectedHosts.isEmpty()) {
            reply.setError(operr("no connected host found for NFS primary storage[uuid:%s]", self.getUuid()));
            bus.reply(msg, reply);
            return;
        }
        String hostUuid = connectedHosts.get(0).getUuid();
        final NfsPrimaryStorageBackend backend = getBackendByHostUuid(hostUuid);
        backend.handle(msg, hostUuid, new ReturnValueCompletion<ScanVmInstanceMetadataFromPrimaryStorageReply>(msg) {
            @Override
            public void success(ScanVmInstanceMetadataFromPrimaryStorageReply r) {
                bus.reply(msg, r);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(CleanupVmInstanceMetadataOnPrimaryStorageMsg msg) {
        CleanupVmInstanceMetadataOnPrimaryStorageReply reply = new CleanupVmInstanceMetadataOnPrimaryStorageReply();
        List<HostInventory> connectedHosts = factory.getConnectedHostForOperation(getSelfInventory());
        if (connectedHosts.isEmpty()) {
            reply.setError(operr("no connected host found for NFS primary storage[uuid:%s]", self.getUuid()));
            bus.reply(msg, reply);
            return;
        }
        String hostUuid = msg.getHostUuid();
        String finalHostUuid = hostUuid;
        if (hostUuid == null || !connectedHosts.stream().anyMatch(h -> h.getUuid().equals(finalHostUuid))) {
            hostUuid = connectedHosts.get(0).getUuid();
        }
        final NfsPrimaryStorageBackend backend = getBackendByHostUuid(hostUuid);
        backend.handle(msg, hostUuid, new ReturnValueCompletion<CleanupVmInstanceMetadataOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CleanupVmInstanceMetadataOnPrimaryStorageReply r) {
                bus.reply(msg, r);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(RebaseVolumeBackingFileOnPrimaryStorageMsg msg) {
        RebaseVolumeBackingFileOnPrimaryStorageReply reply = new RebaseVolumeBackingFileOnPrimaryStorageReply();
        List<HostInventory> connectedHosts = factory.getConnectedHostForOperation(getSelfInventory());
        if (connectedHosts.isEmpty()) {
            reply.setError(operr("no connected host found for NFS primary storage[uuid:%s]", self.getUuid()));
            bus.reply(msg, reply);
            return;
        }
        String hostUuid = connectedHosts.get(0).getUuid();
        final NfsPrimaryStorageBackend backend = getBackendByHostUuid(hostUuid);
        backend.handle(msg, hostUuid, new ReturnValueCompletion<RebaseVolumeBackingFileOnPrimaryStorageReply>(msg) {
            @Override
            public void success(RebaseVolumeBackingFileOnPrimaryStorageReply r) {
                bus.reply(msg, r);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }
}

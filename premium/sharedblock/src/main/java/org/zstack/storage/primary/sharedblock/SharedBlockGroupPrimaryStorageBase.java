package org.zstack.storage.primary.sharedblock;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.asyncbatch.LoopAsyncBatch;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.AutoOffEventCallback;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.cluster.ClusterConnectionStatus;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.metadata.*;
import org.zstack.header.volume.*;
import org.zstack.kvm.*;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageMsg;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageReply;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.storage.volume.VolumeErrors;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.storage.primary.sharedblock.SharedBlockKvmCommands.KVM_HA_CANCEL_SELF_FENCER;
import static org.zstack.storage.primary.sharedblock.SharedBlockKvmCommands.KVM_HA_SETUP_SELF_FENCER;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SharedBlockGroupPrimaryStorageBase extends PrimaryStorageBase implements
        KVMTakeSnapshotExtensionPoint, VolumeSnapshotBeforeDeleteExtensionPoint, KVMBlockCommitExtensionPoint, KVMBlockPullExtensionPoint {
    private static final CLogger logger = Utils.getLogger(SharedBlockGroupPrimaryStorageBase.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private SharedBlockGroupPrimaryStorageImageCacheCleaner imageCacheCleaner;
    @Autowired
    protected SharedBlockGroupPrimaryStorageFactory sharedBlockGroupFactory;
    @Autowired
    private TagManager tagMgr;


    public SharedBlockGroupPrimaryStorageBase() {
    }

    public SharedBlockGroupPrimaryStorageBase(PrimaryStorageVO self) {
        super(self);
    }

    private SharedBlockHypervisorFactory getHypervisorFactoryByHypervisorType(String hvType) {
        for (SharedBlockHypervisorFactory f : pluginRgty.getExtensionList(SharedBlockHypervisorFactory.class)) {
            if (hvType.equals(f.getHypervisorType())) {
                return f;
            }
        }

        throw new CloudRuntimeException(String.format("cannot find SharedBlockHypervisorFactory[type = %s]", hvType));
    }

    @Override
    public void handleLocalMessage(Message msg) {
        if (msg instanceof SharedBlockGroupPrimaryStorageHypervisorSpecificMessage) {
            handle((SharedBlockGroupPrimaryStorageHypervisorSpecificMessage) msg);
        } else if (msg instanceof TakeSnapshotMsg) {
            handle((TakeSnapshotMsg) msg);
        } else if (msg instanceof CheckSnapshotMsg) {
            handle((CheckSnapshotMsg) msg);
        } else if (msg instanceof UploadBitsToBackupStorageMsg) {
            handle((UploadBitsToBackupStorageMsg) msg);
        } else if (msg instanceof BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) {
            handle((BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) msg);
        } else if (msg instanceof CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CreateTemporaryVolumeFromSnapshotMsg) {
            handle((CreateTemporaryVolumeFromSnapshotMsg) msg);
        } else if (msg instanceof SharedBlockRecalculatePrimaryStorageCapacityMsg) {
            handle((SharedBlockRecalculatePrimaryStorageCapacityMsg) msg);
        } else if (msg instanceof DeleteImageCacheOnPrimaryStorageMsg) {
            handle((DeleteImageCacheOnPrimaryStorageMsg) msg);
        } else if (msg instanceof TakeSnapshotOnSharedBlockGroupPrimaryStorageMsg) {
            handle((TakeSnapshotOnSharedBlockGroupPrimaryStorageMsg) msg);
        } else if (msg instanceof MigrateVolumesBetweenSharedBlockGroupPrimaryStorageMsg) {
            handle((MigrateVolumesBetweenSharedBlockGroupPrimaryStorageMsg) msg);
        } else if (msg instanceof SetupSelfFencerOnKvmHostMsg) {
            handle((SetupSelfFencerOnKvmHostMsg) msg);
        } else if (msg instanceof CancelSelfFencerOnKvmHostMsg) {
            handle((CancelSelfFencerOnKvmHostMsg) msg);
        } else if (msg instanceof UpdatePrimaryStorageHostStatusMsg) {
            handle((UpdatePrimaryStorageHostStatusMsg) msg);
        } else if (msg instanceof DownloadBitsFromKVMHostToPrimaryStorageMsg) {
            handle((DownloadBitsFromKVMHostToPrimaryStorageMsg) msg);
        } else if (msg instanceof CancelDownloadBitsFromKVMHostToPrimaryStorageMsg) {
            handle((CancelDownloadBitsFromKVMHostToPrimaryStorageMsg) msg);
        } else if (msg instanceof GetVolumeBackingChainFromPrimaryStorageMsg) {
            handle((GetVolumeBackingChainFromPrimaryStorageMsg) msg);
        } else if ((msg instanceof GetDownloadBitsFromKVMHostProgressMsg)) {
            handle((GetDownloadBitsFromKVMHostProgressMsg) msg);
        } else if (msg instanceof ConfigureFilterMsg) {
            handle((ConfigureFilterMsg) msg);
        } else if (msg instanceof ActivateVolumeOnPrimaryStorageMsg) {
            handle((ActivateVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CommitVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CommitVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof PullVolumeSnapshotOnPrimaryStorageMsg) {
            handle((PullVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    @Override
    public void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddSharedBlockToSharedBlockGroupMsg) {
            handle((APIAddSharedBlockToSharedBlockGroupMsg) msg);
        } else if (msg instanceof APIRefreshSharedblockDeviceCapacityMsg) {
            handle((APIRefreshSharedblockDeviceCapacityMsg) msg);
        } else if (msg instanceof APIUpdateSharedBlockMsg) {
            handle((APIUpdateSharedBlockMsg) msg);
        } else if (msg instanceof APITakeoverPrimaryStorageMsg) {
            handle((APITakeoverPrimaryStorageMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APIUpdateSharedBlockMsg msg) {
        APIUpdateSharedBlockEvent event = new APIUpdateSharedBlockEvent(msg.getId());
        SharedBlockVO sharedBlockVO = Q.New(SharedBlockVO.class)
                .eq(SharedBlockVO_.sharedBlockGroupUuid, msg.getSharedBlockGroupUuid())
                .eq(SharedBlockVO_.uuid, msg.getUuid()).find();

        if (msg.getName() != null) {
            sharedBlockVO.setName(msg.getName());
        }
        if (msg.getDescription() != null) {
            sharedBlockVO.setDescription(msg.getDescription());
        }
        if (msg.getDiskUuid() != null) {
            sharedBlockVO.setDiskUuid(msg.getDiskUuid());
            sharedBlockVO.setName(String.format("disk-%s", msg.getDiskUuid()));
        }

        dbf.update(sharedBlockVO);

        SharedBlockGroupPrimaryStorageInventory inventory = SharedBlockGroupPrimaryStorageInventory.valueOf(
                (SharedBlockGroupVO) Q.New(SharedBlockGroupVO.class).eq(SharedBlockGroupVO_.uuid, msg.getSharedBlockGroupUuid()).find()
        );
        event.setInventory(inventory);
        bus.publish(event);
    }

    private void handle(APIRefreshSharedblockDeviceCapacityMsg msg) {
        APIRefreshSharedBlockDeviceCapacityEvent event = new APIRefreshSharedBlockDeviceCapacityEvent(msg.getId());

        List<PrimaryStorageClusterRefVO> refVOS = Q.New(PrimaryStorageClusterRefVO.class).eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, msg.getSharedBlockGroupUuid()).list();
        if (refVOS == null || refVOS.isEmpty()) {
            SharedBlockGroupPrimaryStorageInventory inventory = SharedBlockGroupPrimaryStorageInventory.valueOf(
                    (SharedBlockGroupVO) Q.New(SharedBlockGroupVO.class).eq(SharedBlockGroupVO_.uuid, msg.getSharedBlockGroupUuid()).find()
            );
            event.setInventory(inventory);
            bus.publish(event);
            return;
        }

        SharedBlockHypervisorBackend bkd = getHypervisorFactoryByClusterUuid(refVOS.get(0).getClusterUuid()).getHypervisorBackend(self);
        bkd.handle(msg, new Completion(msg) {
            @Override
            public void success() {
                SharedBlockGroupPrimaryStorageInventory inventory = SharedBlockGroupPrimaryStorageInventory.valueOf(
                        (SharedBlockGroupVO) Q.New(SharedBlockGroupVO.class).eq(SharedBlockGroupVO_.uuid, msg.getSharedBlockGroupUuid()).find()
                );
                event.setInventory(inventory);
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private void handle(APIAddSharedBlockToSharedBlockGroupMsg msg) {
        APIAddSharedBlockToSharedBlockGroupEvent event = new APIAddSharedBlockToSharedBlockGroupEvent(msg.getId());

        SharedBlockVO blockVO = new SharedBlockVO();
        blockVO.setUuid(Platform.getUuid());
        blockVO.setSharedBlockGroupUuid(msg.getUuid());
        blockVO.setDiskUuid(msg.getDiskUuid());
        blockVO.setType(SharedBlockType.LvmLogicalVolumeBasic);
        blockVO.setName(String.format("disk-%s", msg.getDiskUuid()));
        blockVO.setState(SharedBlockState.Enabled);
        blockVO.setStatus(SharedBlockStatus.Disconnected);
        dbf.persistAndRefresh(blockVO);

        SharedBlockCapacityVO sharedBlockCapacityVO = new SharedBlockCapacityVO();
        sharedBlockCapacityVO.setUuid(blockVO.getUuid());
        dbf.persistAndRefresh(sharedBlockCapacityVO);

        final boolean hasTag = SharedBlockSystemTags.SHARED_BLOCK_FORCE_WIPE.hasTag(msg.getUuid());
        if (!hasTag && msg.hasSystemTag(SharedBlockSystemTags.SHARED_BLOCK_FORCE_WIPE.getTagFormat())) {
            tagMgr.createNonInherentSystemTags(msg.getSystemTags(), msg.getUuid(), PrimaryStorageVO.class.getSimpleName());
        }

        if (msg.hasSystemTag(SharedBlockSystemTags.SHARED_BLOCK_REMOVE_FORCE_WIPE.getTagFormat()) &&
                SharedBlockSystemTags.SHARED_BLOCK_FORCE_WIPE.hasTag(msg.getUuid())) {
            SharedBlockSystemTags.SHARED_BLOCK_FORCE_WIPE.delete(msg.getUuid());
        }

        List<PrimaryStorageClusterRefVO> refVOS = Q.New(PrimaryStorageClusterRefVO.class).eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, msg.getUuid()).list();
        if (refVOS == null || refVOS.isEmpty()) {
            bus.publish(event);
            return;
        }

        SharedBlockHypervisorBackend bkd = getHypervisorFactoryByClusterUuid(refVOS.get(0).getClusterUuid()).getHypervisorBackend(self);
        bkd.addSharedBlockToSharedBlockGroup(msg.getDiskUuid(), msg.getUuid(), new Completion(msg) {
            @Override
            public void success() {
                SharedBlockGroupPrimaryStorageInventory inventory = SharedBlockGroupPrimaryStorageInventory.valueOf(
                        (SharedBlockGroupVO) Q.New(SharedBlockGroupVO.class).eq(SharedBlockGroupVO_.uuid, msg.getUuid()).find()
                );
                event.setInventory(inventory);
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (!hasTag && SharedBlockSystemTags.SHARED_BLOCK_FORCE_WIPE.hasTag(msg.getUuid())) {
                    SharedBlockSystemTags.SHARED_BLOCK_FORCE_WIPE.delete(msg.getUuid());
                }

                dbf.removeByPrimaryKey(blockVO.getUuid(), SharedBlockVO.class);
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    @Override
    protected void handle(APICleanUpImageCacheOnPrimaryStorageMsg msg) {
        APICleanUpImageCacheOnPrimaryStorageEvent evt = new APICleanUpImageCacheOnPrimaryStorageEvent(msg.getId());
        imageCacheCleaner.cleanup(msg.getUuid(), false);
        bus.publish(evt);
    }

    protected SharedBlockHypervisorFactory getHypervisorFactoryByHostUuid(String huuid) {
        String hvType = Q.New(HostVO.class)
                .select(HostVO_.hypervisorType)
                .eq(HostVO_.uuid, huuid)
                .findValue();

        return getHypervisorFactoryByHypervisorType(hvType);
    }

    protected SharedBlockHypervisorFactory getHypervisorFactoryByClusterUuid(String cuuid) {
        String hvType = Q.New(ClusterVO.class)
                .select(ClusterVO_.hypervisorType)
                .eq(ClusterVO_.uuid, cuuid)
                .findValue();

        return getHypervisorFactoryByHypervisorType(hvType);
    }

    protected SharedBlockHypervisorBackend getHypervisorBackendByVolumeUuid(String volUuid) {
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.format);
        q.add(VolumeVO_.uuid, SimpleQuery.Op.EQ, volUuid);
        String format = q.findValue();

        if (format == null) {
            throw new CloudRuntimeException(String.format("cannot find the volume[uuid:%s]", volUuid));
        }

        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(format);
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        return f.getHypervisorBackend(self);
    }

    @Override
    public void attachHook(String clusterUuid, final Completion completion) {
        SharedBlockHypervisorBackend bkd = getHypervisorFactoryByClusterUuid(clusterUuid).getHypervisorBackend(self);
        boolean isTakeover = SharedBlockSystemTags.SHARED_BLOCK_TAKEOVER.hasTag(self.getUuid());
        if (isTakeover) {
            doTakeover(clusterUuid, completion);
        } else {
            bkd.attachHook(clusterUuid, completion);
        }
    }

    protected List<String> getCandidateHostUuidsForTakeover(List<String> clusterUuids) {
        return Q.New(HostVO.class).select(HostVO_.uuid)
                .in(HostVO_.clusterUuid, clusterUuids)
                .eq(HostVO_.state, HostState.Enabled)
                .listValues();
    }

    protected void doTakeover(String clusterUuid, Completion completion) {
        List<String> candidateHostUuids = getCandidateHostUuidsForTakeover(Collections.singletonList(clusterUuid));
        if (candidateHostUuids.isEmpty()) {
            completion.fail(operr("no connected host available for takeover of primary storage[uuid:%s]", self.getUuid()));
            return;
        }

        SharedBlockHypervisorFactory f = getHypervisorFactoryByClusterUuid(clusterUuid);
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        tryTakeoverOnNextHost(candidateHostUuids.iterator(), bkd, new Completion(completion) {
            @Override
            public void success() {
                SharedBlockSystemTags.SHARED_BLOCK_TAKEOVER.delete(self.getUuid());
                SharedBlockSystemTags.SHARED_BLOCK_NOT_INITIALIZED.delete(self.getUuid());
                bkd.attachHook(clusterUuid, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        }, new ArrayList<>());
    }

    /**
     * Core takeover retry loop: tries each candidate host in order until one succeeds.
     * On success, calls completion.success(); on all-hosts-exhausted, calls completion.fail().
     */
    private void tryTakeoverOnNextHost(Iterator<String> hostIterator, SharedBlockHypervisorBackend bkd,
                                       Completion completion, List<ErrorCode> errors) {
        if (!hostIterator.hasNext()) {
            completion.fail(operr("takeover primary storage[uuid:%s] failed on all candidate hosts: %s",
                    self.getUuid(), errors));
            return;
        }

        String hostUuid = hostIterator.next();
        logger.info(String.format("attempting takeover of primary storage[uuid:%s] on host[uuid:%s]",
                self.getUuid(), hostUuid));

        bkd.takeover(hostUuid, new ReturnValueCompletion<SharedBlockKvmCommands.TakeoverRsp>(completion) {
            @Override
            public void success(SharedBlockKvmCommands.TakeoverRsp rsp) {
                logger.info(String.format("takeover of primary storage[uuid:%s] succeeded on host[uuid:%s]",
                        self.getUuid(), hostUuid));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("takeover of primary storage[uuid:%s] failed on host[uuid:%s]: %s",
                        self.getUuid(), hostUuid, errorCode));
                errors.add(errorCode);
                tryTakeoverOnNextHost(hostIterator, bkd, completion, errors);
            }
        });
    }

    @Override
    public void detachHook(String clusterUuid, final Completion completion) {
        SharedBlockHypervisorBackend bkd = getHypervisorFactoryByClusterUuid(clusterUuid).getHypervisorBackend(self);
        bkd.detachHook(clusterUuid, completion);
    }

    @Override
    protected void handle(DownloadVolumeTemplateToPrimaryStorageMsg msg) {
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHostUuid(msg.getHostUuid());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DownloadVolumeTemplateToPrimaryStorageReply>(msg) {
            @Override
            public void success(DownloadVolumeTemplateToPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DownloadImageToPrimaryStorageCacheReply reply = new DownloadImageToPrimaryStorageCacheReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final InstantiateVolumeOnPrimaryStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                if (msg.getDestHost() == null) {
                    List<HostInventory> hostUuids = sharedBlockGroupFactory.getConnectedHostsForOperation(msg.getPrimaryStorageUuid());
                    if (hostUuids == null) {
                        throw new OperationFailureException(operr("the shared mount point primary storage[uuid:%s, name:%s] cannot find any " +
                                "available host in attached clusters for instantiating the volume", self.getUuid(), self.getName()));
                    }

                    msg.setDestHost(HostInventory.valueOf(dbf.findByUuid(hostUuids.get(0).getUuid(), HostVO.class)));
                }

                SharedBlockHypervisorFactory f = getHypervisorFactoryByHostUuid(msg.getDestHost().getUuid());
                SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
                bkd.handle(msg, new ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply>(msg) {
                    @Override
                    public void success(InstantiateVolumeOnPrimaryStorageReply reply) {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return String.format("instantiate-volume-on-sharedblock-primaryStorage-%s", msg.getPrimaryStorageUuid());
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            protected int getSyncLevel() {
                return SharedBlockGlobalConfig.VOLUME_INSTANTIATE_SYNC_LEVEL.value(Integer.class);
            }
        });
    }

    @Override
    protected void handle(final DeleteVolumeOnPrimaryStorageMsg msg) {
        HypervisorType type = findHypervisorTypeByImageFormatAndPrimaryStorageUuid(msg.getVolume().getFormat(), msg.getPrimaryStorageUuid());
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        final SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteVolumeOnPrimaryStorageReply reply) {
                logger.debug(String.format("successfully delete volume[uuid:%s]", msg.getVolume().getUuid()));
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
                if (errorCode.isError(VolumeErrors.VOLUME_IN_USE)) {
                    logger.debug(String.format("can't delete volume[uuid:%s] right now, skip this GC job because it's in use", msg.getVolume().getUuid()));
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                    return;
                }

                logger.debug(String.format("can't delete volume[uuid:%s] right now, add a GC job", msg.getVolume().getUuid()));
                SharedBlockDeleteVolumeGC gc = new SharedBlockDeleteVolumeGC();
                gc.NAME = String.format("gc-shared-block-%s-volume-%s", self.getUuid(), msg.getVolume().getInstallPath());
                gc.primaryStorageUuid = self.getUuid();
                gc.hypervisorType = type.toString();
                gc.volume = msg.getVolume();
                gc.deduplicateSubmit(SharedBlockGlobalConfig.GC_INTERVAL.value(Long.class), TimeUnit.SECONDS);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(DeleteBitsOnPrimaryStorageMsg msg) {
        final DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getFormat());
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        final SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.deleteBits(msg.getInstallPath(), new Completion(msg) {
            @Override
            public void success() {
                logger.debug(String.format("successfully delete bits[uuid:%s]", msg.getInstallPath()));
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg) {
        HypervisorType type = findHypervisorTypeByImageFormatAndPrimaryStorageUuid(msg.getVolumeInventory().getFormat(), msg.getPrimaryStorageUuid());
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<CreateImageCacheFromVolumeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CreateImageCacheFromVolumeOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateImageCacheFromVolumeOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        HypervisorType type = findHypervisorTypeByImageFormatAndPrimaryStorageUuid(msg.getVolumeSnapshot().getFormat(), msg.getPrimaryStorageUuid());
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateImageCacheFromVolumeOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        HypervisorType type = findHypervisorTypeByImageFormatAndPrimaryStorageUuid(msg.getVolumeInventory().getFormat(), msg.getPrimaryStorageUuid());
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<CreateTemplateFromVolumeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CreateTemplateFromVolumeOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DownloadDataVolumeToPrimaryStorageMsg msg) {
        HypervisorType type = findHypervisorTypeByImageFormatAndPrimaryStorageUuid(msg.getImage().getFormat(), msg.getPrimaryStorageUuid());
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DownloadDataVolumeToPrimaryStorageReply>(msg) {
            @Override
            public void success(DownloadDataVolumeToPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(GetInstallPathForDataVolumeDownloadMsg msg) {
        HypervisorType type = findHypervisorTypeByImageFormatAndPrimaryStorageUuid(msg.getImage().getFormat(), msg.getPrimaryStorageUuid());
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<GetInstallPathForDataVolumeDownloadReply>(msg) {
            @Override
            public void success(GetInstallPathForDataVolumeDownloadReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                GetInstallPathForDataVolumeDownloadReply reply = new GetInstallPathForDataVolumeDownloadReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DeleteVolumeBitsOnPrimaryStorageMsg msg) {
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(msg.getHypervisorType());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DeleteVolumeBitsOnPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteVolumeBitsOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DeleteVolumeBitsOnPrimaryStorageReply reply = new DeleteVolumeBitsOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DownloadIsoToPrimaryStorageMsg msg) {
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHostUuid(msg.getDestHostUuid());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DownloadIsoToPrimaryStorageReply>(msg) {
            @Override
            public void success(DownloadIsoToPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode error) {
                DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
                reply.setError(error);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DeleteIsoFromPrimaryStorageMsg msg) {
        HypervisorType type = findHypervisorTypeByImageFormatAndPrimaryStorageUuid(msg.getIsoSpec().getInventory().getFormat(), msg.getPrimaryStorageUuid());
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DeleteIsoFromPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteIsoFromPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode error) {
                DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
                reply.setError(error);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {
        AskVolumeSnapshotCapabilityReply reply = new AskVolumeSnapshotCapabilityReply();
        VolumeSnapshotCapability capability = new VolumeSnapshotCapability();

        String volumeType = msg.getVolume().getType();
        if (VolumeType.Data.toString().equals(volumeType) || VolumeType.Root.toString().equals(volumeType)) {
            // We don't support snapshot for shared volume on SharedBlock
            // c.f. https://bugzilla.redhat.com/show_bug.cgi?id=1518517
            capability.setSupport(!msg.getVolume().isShareable());
            capability.setArrangementType(VolumeSnapshotCapability.VolumeSnapshotArrangementType.CHAIN);
        } else if (VolumeType.Memory.toString().equals(volumeType)) {
            capability.setSupport(true);
            capability.setArrangementType(VolumeSnapshotCapability.VolumeSnapshotArrangementType.INDIVIDUAL);
        } else {
            throw new CloudRuntimeException(String.format("unknown volume type %s", volumeType));
        }

        reply.setCapability(capability);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(final SyncVolumeSizeOnPrimaryStorageMsg msg) {
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.format);
        q.add(VolumeVO_.uuid, SimpleQuery.Op.EQ, msg.getVolumeUuid());
        String format = q.findValue();

        HypervisorType type = findHypervisorTypeByImageFormatAndPrimaryStorageUuid(format, msg.getPrimaryStorageUuid());
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(SyncVolumeSizeOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
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
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.format);
        q.add(VolumeVO_.uuid, SimpleQuery.Op.EQ, msg.getVolumeUuid());
        String format = q.findValue();

        HypervisorType type = findHypervisorTypeByImageFormatAndPrimaryStorageUuid(format, msg.getPrimaryStorageUuid());
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<EstimateVolumeTemplateSizeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(EstimateVolumeTemplateSizeOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
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
    protected void handle(BatchSyncVolumeSizeOnPrimaryStorageMsg msg) {
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHostUuid(msg.getHostUuid());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<BatchSyncVolumeSizeOnPrimaryStorageReply>(msg) {
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

    @Override
    protected void handle(final DeleteSnapshotOnPrimaryStorageMsg msg) {
        DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
        List<String> attachedClusterUuids = getSelfInventory().getAttachedClusterUuids();
        if (attachedClusterUuids == null) {
            reply.setError(Platform.operr(
                    "can not found any cluster attached on shared block group primary storage[uuid: %S]", getSelfInventory().getUuid()));
            bus.reply(msg, reply);
        } else {
            // TODO(WeiW): Here we assume all cluster attach ps must be same HypervisorType
            SharedBlockHypervisorFactory f = getHypervisorFactoryByClusterUuid(attachedClusterUuids.get(0));
            SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
            bkd.handle(msg, new ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply>(msg) {
                @Override
                public void success(DeleteSnapshotOnPrimaryStorageReply returnValue) {
                    bus.reply(msg, returnValue);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        }
    }

    @Override
    protected void handle(final RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        SharedBlockHypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolume().getUuid());
        bkd.handle(msg, new ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(RevertVolumeFromSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        SharedBlockHypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolume().getUuid());
        bkd.handle(msg, new ReturnValueCompletion<ReInitRootVolumeFromTemplateOnPrimaryStorageReply>(msg) {
            @Override
            public void success(ReInitRootVolumeFromTemplateOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
        SharedBlockHypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getTo().getUuid());
        MergeVolumeSnapshotOnPrimaryStorageReply reply = new MergeVolumeSnapshotOnPrimaryStorageReply();
        bkd.stream(msg.getFrom(), msg.getTo(), msg.isFullRebase(), new Completion(msg) {
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
        SharedBlockHypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolume().getUuid());
        FlattenVolumeOnPrimaryStorageReply reply = new FlattenVolumeOnPrimaryStorageReply();
        bkd.stream(null, msg.getVolume(), true, new Completion(msg) {
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

    private void handle(SharedBlockGroupPrimaryStorageHypervisorSpecificMessage msg) {
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(msg.getHypervisorType());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handleHypervisorSpecificMessage(msg);
    }

    private void handle(final CheckSnapshotMsg msg) {
        TakeSnapshotReply reply = new TakeSnapshotReply();
        SharedBlockHypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolumeUuid());
        bkd.handle(msg, new Completion(msg) {
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

    private void handle(final TakeSnapshotMsg msg) {
        final VolumeSnapshotInventory sp = msg.getStruct().getCurrent();
        SharedBlockHypervisorBackend bkd = getHypervisorBackendByVolumeUuid(sp.getVolumeUuid());
        bkd.handle(msg, new ReturnValueCompletion<TakeSnapshotReply>(msg) {
            @Override
            public void success(TakeSnapshotReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                TakeSnapshotReply reply = new TakeSnapshotReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final UploadBitsToBackupStorageMsg msg) {
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(msg.getHypervisorType());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<UploadBitsToBackupStorageReply>(msg) {
            @Override
            public void success(UploadBitsToBackupStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                UploadBitsToBackupStorageReply reply = new UploadBitsToBackupStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg) {
        SharedBlockHypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getSnapshot().getVolumeUuid());
        bkd.handle(msg, new ReturnValueCompletion<BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply>(msg) {
            @Override
            public void success(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply reply = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        SharedBlockHypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getSnapshot().getVolumeUuid());
        bkd.handle(msg, new ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply>(msg) {
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

    private void handle(final CreateTemporaryVolumeFromSnapshotMsg msg) {
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(msg.getHypervisorType());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply>(msg) {
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

    private void deactivateVolume(String hostUuid, String psUuid, String installPath, WhileCompletion completion) {
        deactivateVolume(hostUuid, psUuid, installPath, completion, true);
    }

    private void deactivateVolume(String hostUuid, String psUuid, String installPath, WhileCompletion completion, boolean recursive) {
        SharedBlockKvmCommands.ActiveVolumeCmd cmd = new SharedBlockKvmCommands.ActiveVolumeCmd();
        cmd.lockType = LvmlockdLockingType.NULL.getValue();
        cmd.installPath = installPath;
        cmd.vgUuid = psUuid;
        cmd.recursive = recursive;

        new KvmCommandSender(hostUuid).send(cmd, SharedBlockKvmCommands.CHANGE_VOLUME_ACTIVE_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper w) {
                SharedBlockKvmCommands.AgentRsp rsp = w.getResponse(SharedBlockKvmCommands.AgentRsp.class);
                return rsp.success ? null : operr("%s", rsp.error);
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper w) {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("deactivate volume %s failed on host %s", installPath, hostUuid));
                completion.done();
            }
        });
    }

    private void handle(final DeleteImageCacheOnPrimaryStorageMsg msg) {
        DeleteImageCacheOnPrimaryStorageReply sreply = new DeleteImageCacheOnPrimaryStorageReply();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-image-cache-on-shared-block-group-primary-storage-%s", msg.getPrimaryStorageUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "deactivate-lv-on-hosts";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<HostInventory> hosts = sharedBlockGroupFactory.getConnectedHostsForOperation(msg.getPrimaryStorageUuid());
                new While<>(hosts)
                        .step((hostInv, comp) -> deactivateVolume(hostInv.getUuid(), msg.getPrimaryStorageUuid(), msg.getInstallPath(), comp), 5)
                        .run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
            }
        });
        chain.then(new NoRollbackFlow() {
            String __name__ = "delete-volume-cache";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                String hostUuid = getAvailableHostUuidForOperation();
                if (hostUuid == null) {
                    trigger.next();
                    return;
                }

                HostVO hvo = dbf.findByUuid(hostUuid, HostVO.class);
                DeleteVolumeBitsOnPrimaryStorageMsg dmsg = new DeleteVolumeBitsOnPrimaryStorageMsg();
                dmsg.setFolder(false);
                dmsg.setHypervisorType(hvo.getHypervisorType());
                dmsg.setInstallPath(msg.getInstallPath());
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

    @Transactional(readOnly = true)
    public String getAvailableHostUuidForOperation() {
        String sql = "select host.uuid from PrimaryStorageClusterRefVO ref, HostVO host where" +
                " ref.clusterUuid = host.clusterUuid and ref.primaryStorageUuid = :psUuid and host.status = :hstatus" +
                " and host.state = :hstate";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("psUuid", self.getUuid());
        q.setParameter("hstatus", HostStatus.Connected);
        q.setParameter("hstate", HostState.Enabled);
        List<String> hostUuids = q.getResultList();
        if (hostUuids.isEmpty()) {
            return null;
        }

        Collections.shuffle(hostUuids);
        return hostUuids.get(0);
    }

    protected void hookToKVMHostConnectedEventToChangeStatusToConnected(){
        // hook on host connected event to reconnect the primary storage once there is
        // one host connected in attached clusters
        evtf.onLocal(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, new AutoOffEventCallback() {
            {
                uniqueIdentity = String.format("connect-sharedblock-%s-when-host-connected", self.getUuid());
            }

            @Override
            protected boolean run(Map tokens, Object data) {
                HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
                if (!HostStatus.Connected.toString().equals(d.getNewStatus())) {
                    return false;
                }

                // TODO(WeiW): Should check all SharedBlockHypervisorBackend or something else rather than hard code
                if (!KVMConstant.KVM_HYPERVISOR_TYPE.equals(d.getInventory().getHypervisorType())) {
                    return false;
                }

                self = dbf.reload(self);
                if (self.getStatus() == PrimaryStorageStatus.Connected) {
                    return true;
                }

                if (self.getAttachedClusterRefs().stream()
                        .noneMatch(ref -> ref.getClusterUuid().equals(d.getInventory().getClusterUuid()))) {
                    return false;
                }

                FutureCompletion future = new FutureCompletion(null);

                ConnectParam p = new ConnectParam();
                p.setNewAdded(false);
                connectHook(p, future);

                future.await();

                if (!future.isSuccess()) {
                    logger.warn(String.format("unable to reconnect the shared block group primary storage[uuid:%s, name:%s], %s",
                            self.getUuid(), self.getName(), future.getErrorCode()));
                } else {
                    changeStatus(PrimaryStorageStatus.Connected);
                }

                return future.isSuccess();
            }
        });
    }

    @Override
    protected void connectHook(ConnectParam param, final Completion completion) {
        List<String> clusterUuids = self.getAttachedClusterRefs().stream()
                .map(PrimaryStorageClusterRefVO::getClusterUuid)
                .collect(Collectors.toList());

        if (!clusterUuids.isEmpty()) {
            clusterUuids = Q.New(HostVO.class).select(HostVO_.clusterUuid)
                    .eq(HostVO_.status, HostStatus.Connected)
                    .in(HostVO_.clusterUuid, clusterUuids)
                    .listValues();
        }

        if (clusterUuids.isEmpty()){
            if (!param.isNewAdded()){
                hookToKVMHostConnectedEventToChangeStatusToConnected();
            }

            String err = i18n("the shared block group primary storage[uuid:%s, name:%s] has not attached to any clusters, " +
                    "or no hosts in the attached clusters are connected", self.getUuid(), self.getName());
            completion.fail(err(PrimaryStorageErrors.DISCONNECTED, err));
            return;
        }

        final List<String> finalClusterUuids = clusterUuids.stream().distinct().collect(Collectors.toList());
        new LoopAsyncBatch<String>(completion) {
            boolean success;

            @Override
            protected Collection<String> collect() {
                return finalClusterUuids;
            }

            @Override
            protected AsyncBatchRunner forEach(String item) {
                return new AsyncBatchRunner() {
                    @Override
                    public void run(NoErrorCompletion completion) {
                        SharedBlockHypervisorBackend bkd = getHypervisorFactoryByClusterUuid(item).getHypervisorBackend(self);
                        bkd.connectByClusterUuid(item, false, new ReturnValueCompletion<ClusterConnectionStatus>(completion) {
                            @Override
                            public void success(ClusterConnectionStatus clusterStatus) {
                                // isConnectedHostInCluster has been checked before
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
                    completion.success();
                } else {
                    completion.fail(errf.stringToOperationError(
                            i18n("failed to connect to all clusters%s", finalClusterUuids), errors
                    ));
                }
            }
        }.start();
    }

    @Override
    protected void pingHook(Completion completion) {
        List<String> clusterUuids = Q.New(PrimaryStorageClusterRefVO.class)
                .select(PrimaryStorageClusterRefVO_.clusterUuid)
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, self.getUuid())
                .listValues();
        if (clusterUuids.size() == 0) {
            completion.success();
            return;
        }

        List<String> operationalHostUuids = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .in(HostVO_.clusterUuid, clusterUuids)
                .notIn(HostVO_.state, Arrays.asList(HostState.Maintenance, HostState.PreMaintenance))
                .listValues();
        if (operationalHostUuids.size() == 0) {
            completion.success();
            return;
        }

        Set<String> psHostStatus = Q.New(SharedBlockGroupPrimaryStorageHostRefVO.class)
                .eq(SharedBlockGroupPrimaryStorageHostRefVO_.primaryStorageUuid, self.getUuid())
                .select(SharedBlockGroupPrimaryStorageHostRefVO_.status)
                .listValues().stream().map(Object::toString).collect(Collectors.toSet());

        if (!psHostStatus.contains(PrimaryStorageHostStatus.Connected.toString()) &&
                psHostStatus.contains(PrimaryStorageHostStatus.Disconnected.toString())) {
            completion.fail(operr("the SharedBlock primary storage[uuid:%s, name:%s] has not attached to any clusters, or no hosts in the" +
                    " attached clusters are connected", self.getUuid(), self.getName()));
            return;
        }

        SharedBlockHypervisorFactory f = getHypervisorFactoryByClusterUuid(clusterUuids.get(0));
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.ping(self.getUuid(), completion);
    }

    @Override
    protected void handle(ShrinkVolumeSnapshotOnPrimaryStorageMsg msg) {
        ShrinkVolumeSnapshotOnPrimaryStorageReply reply = new ShrinkVolumeSnapshotOnPrimaryStorageReply();
        VolumeSnapshotVO snapshotVO = dbf.findByUuid(msg.getSnapshotUuid(), VolumeSnapshotVO.class);
        if (snapshotVO == null) {
            throw new OperationFailureException(err(SysErrors.RESOURCE_NOT_FOUND,
                    "cannot find volume snapshot[uuid:%s]", msg.getSnapshotUuid()
            ));
        }

        SharedBlockHypervisorBackend bkd = getHypervisorBackendByVolumeUuid(snapshotVO.getVolumeUuid());
        bkd.handle(msg, new ReturnValueCompletion<ShrinkVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(ShrinkVolumeSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg) {
        VolumeSnapshotVO snapshotVO = dbf.findByUuid(msg.getSnapshotUuid(), VolumeSnapshotVO.class);
        if (snapshotVO == null) {
            throw new OperationFailureException(err(SysErrors.RESOURCE_NOT_FOUND,
                    "cannot find volume snapshot[uuid:%s]", msg.getSnapshotUuid()
            ));
        }
        SharedBlockHypervisorBackend bkd = getHypervisorBackendByVolumeUuid(snapshotVO.getVolumeUuid());
        bkd.handle(msg, new ReturnValueCompletion<GetVolumeSnapshotEncryptedOnPrimaryStorageReply>(msg) {
            @Override
            public void success(GetVolumeSnapshotEncryptedOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
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
        List<String> attachedClusterUuids = getSelfInventory().getAttachedClusterUuids();
        if (attachedClusterUuids == null) {
            PhysicalCapacityUsage usage = new PhysicalCapacityUsage();
            usage.availablePhysicalSize = 0;
            usage.totalPhysicalSize = 0;
            completion.success(usage);
        } else {
            // TODO(WeiW): Here we assume all cluster attach ps must be same HypervisorType
            SharedBlockHypervisorFactory f = getHypervisorFactoryByClusterUuid(attachedClusterUuids.get(0));
            SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
            bkd.getPhysicalCapacity(getSelfInventory(), completion);
        }
    }

    @Override
    public void beforeTakeSnapshot(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, Completion completion) {
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(host.getHypervisorType());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.beforeSnapshotTake(host, msg, cmd, completion);
    }

    @Override
    public void afterTakeSnapshot(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, KVMAgentCommands.TakeSnapshotResponse rsp) {
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(host.getHypervisorType());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.afterSnapshotTake(host, msg, cmd, rsp);
    }

    @Override
    public void afterTakeSnapshotFailed(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, KVMAgentCommands.TakeSnapshotResponse rsp, ErrorCode err) {
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(host.getHypervisorType());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.afterSnapshotTakeFailed(host, msg, cmd, rsp, err);
    }

    @Override
    public void volumeSnapshotBeforeDeleteExtensionPoint(VolumeSnapshotInventory snapshot) {
        if (!Q.New(SharedBlockGroupVO.class)
                .eq(SharedBlockGroupVO_.uuid, snapshot.getPrimaryStorageUuid())
                .isExists()) {
            return;
        }

        logger.debug(String.format("prepare-shared-block-volume-snapshot-%s-delete", snapshot.getUuid()));

        FutureCompletion completion = new FutureCompletion(null);
        List<HostInventory> hosts = sharedBlockGroupFactory.getConnectedHostsForOperation(snapshot.getPrimaryStorageUuid());
        new While<>(hosts).step((hostInv, comp) -> deactivateVolume(hostInv.getUuid(), snapshot.getPrimaryStorageUuid(),
                snapshot.getPrimaryStorageInstallPath(), comp, false), 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    @Override
    public void handle(AskInstallPathForNewSnapshotMsg msg) {
        String hvType = Q.New(HostVO.class)
                .select(HostVO_.hypervisorType)
                .eq(HostVO_.uuid, msg.getHostUuid())
                .findValue();

        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(hvType);
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<AskInstallPathForNewSnapshotReply>(msg) {
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

    private void handle(TakeSnapshotOnSharedBlockGroupPrimaryStorageMsg msg) {
        SharedBlockHypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolumeUuid());
        bkd.handle(msg, new ReturnValueCompletion<TakeSnapshotOnSharedBlockGroupPrimaryStorageReply>(msg) {
            @Override
            public void success(TakeSnapshotOnSharedBlockGroupPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                TakeSnapshotOnSharedBlockGroupPrimaryStorageReply reply = new TakeSnapshotOnSharedBlockGroupPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(MigrateVolumesBetweenSharedBlockGroupPrimaryStorageMsg msg) {
        if (msg.getMigrateVolumeStructs() == null || msg.getMigrateVolumeStructs().isEmpty()) {
            throw new OperationFailureException(operr("empty migrateVolumeStructs in migrateVolumesBetweenSharedBlockGroupPrimaryStorageMsg!"));
        }

        Optional<SharedBlockMigrateVolumeStruct> opt = msg.getMigrateVolumeStructs().stream().filter(s -> s.getVolumeUuid() != null).findAny();

        if (!opt.isPresent()) {
            throw new OperationFailureException(operr("no volume in migrateVolumeStructs in migrateVolumesBetweenSharedBlockGroupPrimaryStorageMsg!"));
        }

        SharedBlockHypervisorBackend bkd = getHypervisorBackendByVolumeUuid(opt.get().getVolumeUuid());
        bkd.handle(msg, new ReturnValueCompletion<MigrateVolumesBetweenSharedBlockGroupPrimaryStorageReply>(msg) {
            @Override
            public void success(MigrateVolumesBetweenSharedBlockGroupPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                MigrateVolumesBetweenSharedBlockGroupPrimaryStorageReply reply = new MigrateVolumesBetweenSharedBlockGroupPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(ActivateVolumeOnPrimaryStorageMsg msg) {
        ActivateVolumeOnPrimaryStorageReply reply = new ActivateVolumeOnPrimaryStorageReply();
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(msg.getHypervisorType());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new Completion(msg) {
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

    private void handle(ConfigureFilterMsg msg) {
        SharedBlockHypervisorFactory factoty = getHypervisorFactoryByClusterUuid(msg.getClusterUuid());
        SharedBlockHypervisorBackend bkd = factoty.getHypervisorBackend(self);
        ConfigureFilterReply reply = new ConfigureFilterReply();
        bkd.handle(msg, new Completion(msg) {
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

    private void handle(CancelSelfFencerOnKvmHostMsg msg) {
        KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam param = msg.getParam();
        SharedBlockKvmCommands.KvmCancelSelfFencerCmd cmd = new SharedBlockKvmCommands.KvmCancelSelfFencerCmd();
        cmd.vgUuid = msg.getPrimaryStorageUuid();
        cmd.hostUuid = param.getHostUuid();

        CancelSelfFencerOnKvmHostReply reply = new CancelSelfFencerOnKvmHostReply();
        new KvmCommandSender(param.getHostUuid()).send(cmd, KVM_HA_CANCEL_SELF_FENCER, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper w) {
                SharedBlockKvmCommands.AgentRsp rsp = w.getResponse(SharedBlockKvmCommands.AgentRsp.class);
                return rsp.success ? null : operr("%s", rsp.error);
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(msg) {
            @Override
            public void success(KvmResponseWrapper w) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setSuccess(false);
                reply.setError(errorCode);
                logger.warn("cancel self fencer failed");
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final SetupSelfFencerOnKvmHostMsg msg) {
        KvmSetupSelfFencerExtensionPoint.KvmSetupSelfFencerParam param = msg.getParam();
        SharedBlockKvmCommands.KvmSetupSelfFencerCmd cmd = new SharedBlockKvmCommands.KvmSetupSelfFencerCmd();
        cmd.hostUuid = param.getHostUuid();
        cmd.interval = param.getInterval();
        cmd.maxAttempts = param.getMaxAttempts();
        cmd.storageCheckerTimeout = param.getStorageCheckerTimeout();
        cmd.vgUuid = param.getPrimaryStorage().getUuid();
        cmd.strategy = param.getStrategy();
        cmd.fencers = param.getFencers();

        final SetupSelfFencerOnKvmHostReply reply = new SetupSelfFencerOnKvmHostReply();
        new KvmCommandSender(param.getHostUuid()).send(cmd, KVM_HA_SETUP_SELF_FENCER, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                SharedBlockKvmCommands.AgentRsp rsp = wrapper.getResponse(SharedBlockKvmCommands.AgentRsp.class);
                return rsp.success ? null : operr("%s", rsp.error);
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(msg) {
            @Override
            public void success(KvmResponseWrapper wrapper) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }


    private void handle(DownloadBitsFromKVMHostToPrimaryStorageMsg msg) {
        String hvType = Q.New(HostVO.class)
                .select(HostVO_.hypervisorType)
                .eq(HostVO_.uuid, msg.getSrcHostUuid())
                .findValue();

        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(hvType);
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DownloadBitsFromKVMHostToPrimaryStorageReply>(msg) {
            @Override
            public void success(DownloadBitsFromKVMHostToPrimaryStorageReply returnValue) {
                logger.info(String.format("successfully downloaded bits %s from kvm host %s to primary storage %s", msg.getHostInstallPath(), msg.getSrcHostUuid(), msg.getPrimaryStorageUuid()));
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("failed to download bits %s from kvm host %s to primary storage %s", msg.getHostInstallPath(), msg.getSrcHostUuid(), msg.getPrimaryStorageUuid()));
                DownloadBitsFromKVMHostToPrimaryStorageReply reply = new DownloadBitsFromKVMHostToPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    public void handle(CancelDownloadBitsFromKVMHostToPrimaryStorageMsg msg) {
        String hvType = Q.New(HostVO.class)
                .select(HostVO_.hypervisorType)
                .eq(HostVO_.uuid, msg.getDestHostUuid())
                .findValue();

        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(hvType);
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<CancelDownloadBitsFromKVMHostToPrimaryStorageReply>(msg) {
            @Override
            public void success(CancelDownloadBitsFromKVMHostToPrimaryStorageReply returnValue) {
                logger.info(String.format("successfully cancel downloaded bits to primary storage %s", msg.getPrimaryStorageUuid()));
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("failed to cancel download bits to primary storage %s", msg.getPrimaryStorageUuid()));
                CancelDownloadBitsFromKVMHostToPrimaryStorageReply reply = new CancelDownloadBitsFromKVMHostToPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    public void handle(GetDownloadBitsFromKVMHostProgressMsg msg) {
        String hvType = Q.New(HostVO.class)
                .select(HostVO_.hypervisorType)
                .eq(HostVO_.uuid, msg.getHostUuid())
                .findValue();

        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(hvType);
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<GetDownloadBitsFromKVMHostProgressReply>(msg) {
            @Override
            public void success(GetDownloadBitsFromKVMHostProgressReply returnValue) {
                logger.info(String.format("successfully get downloaded bits progress from primary storage %s", msg.getPrimaryStorageUuid()));
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("failed to get downloaded bits progress from primary storage %s", msg.getPrimaryStorageUuid()));
                GetDownloadBitsFromKVMHostProgressReply reply = new GetDownloadBitsFromKVMHostProgressReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    public void handle(UpdatePrimaryStorageHostStatusMsg msg){
        SharedBlockGroupPrimaryStorageHostRefVO refVO = Q.New(SharedBlockGroupPrimaryStorageHostRefVO.class)
                .eq(SharedBlockGroupPrimaryStorageHostRefVO_.primaryStorageUuid, msg.getPrimaryStorageUuid())
                .eq(SharedBlockGroupPrimaryStorageHostRefVO_.hostUuid, msg.getHostUuid())
                .find();

        if (refVO == null) {
            logger.warn(String.format("can not find ref of host uuid %s and ps %s", msg.getHostUuid(), msg.getPrimaryStorageUuid()));
            refVO = new SharedBlockHostIdGetter().getHostIdRef(msg.getHostUuid(), msg.getPrimaryStorageUuid());
        }

        PrimaryStorageHostStatus oldStatus = refVO.getStatus();
        if (!refVO.getStatus().equals(msg.getStatus())) {
            logger.debug(String.format("sharedblock group primary storage %s on host %s status was %s, update to %s",
                    msg.getPrimaryStorageUuid(), msg.getHostUuid(), refVO.getStatus(), msg.getStatus()));
            refVO.setStatus(msg.getStatus());
            refVO = dbf.updateAndRefresh(refVO);
        }

        // TODO(weiw): implement pinghost plugin
        if (msg.getStatus().equals(PrimaryStorageHostStatus.Disconnected) &&
                SharedBlockGlobalConfig.DISABLE_HOST_PS_FAILED.value().equals("true")) {
            ChangeHostStateMsg cmsg = new ChangeHostStateMsg();
            cmsg.setUuid(msg.getHostUuid());
            cmsg.setStateEvent(HostStateEvent.disable.toString());
            bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, cmsg.getHostUuid());
            bus.send(cmsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    ChangeHostStateReply r = reply.isSuccess() ? reply.castReply() : null;
                    if (r == null || !r.isSuccess()) {
                        logger.warn(String.format("failed to change host[uuid: %s] state by event(%s), %s",
                                cmsg.getHostUuid(), HostStateEvent.disable, r == null ? reply.getError() : r.getError()));
                        return;
                    }

                    logger.debug(String.format("successfully changed host[uuid: %s] state by event(%s)",
                            cmsg.getHostUuid(), HostStateEvent.disable));
                }
            });
        }

        PrimaryStorageCanonicalEvent.PrimaryStorageHostStatusChangeData data = new PrimaryStorageCanonicalEvent.PrimaryStorageHostStatusChangeData();
        data.setHostUuid(msg.getHostUuid());
        data.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        data.setOldStatus(oldStatus);
        data.setNewStatus(refVO.getStatus());
        data.setReason(msg.getReason());
        evtf.fire(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_HOST_STATUS_CHANGED_PATH, data);
    }

    private void handle(GetVolumeBackingChainFromPrimaryStorageMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getVolumeFormat());
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        final SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<GetVolumeBackingChainFromPrimaryStorageReply>(msg) {
            @Override
            public void success(GetVolumeBackingChainFromPrimaryStorageReply reply) {
                logger.info(String.format("successfully get volume backing chain from primary storage %s", msg.getPrimaryStorageUuid()));
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("failed to get volume backing chain from primary storage %s", msg.getPrimaryStorageUuid()));
                GetVolumeBackingChainFromPrimaryStorageReply reply = new GetVolumeBackingChainFromPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final CommitVolumeSnapshotOnPrimaryStorageMsg msg) {
        SharedBlockHypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolume().getUuid());
        bkd.handle(msg, new ReturnValueCompletion<CommitVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CommitVolumeSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CommitVolumeSnapshotOnPrimaryStorageReply reply = new CommitVolumeSnapshotOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final PullVolumeSnapshotOnPrimaryStorageMsg msg) {
        SharedBlockHypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolume().getUuid());
        bkd.handle(msg, new ReturnValueCompletion<PullVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(PullVolumeSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                PullVolumeSnapshotOnPrimaryStorageReply reply = new PullVolumeSnapshotOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    public HypervisorType findHypervisorTypeByImageFormatAndPrimaryStorageUuid(String imageFormat, final String psUuid) {
        HypervisorType hvType = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(imageFormat);
        if (hvType != null) {
            return hvType;
        }

        String type = new Callable<String>() {
            @Override
            @Transactional(readOnly = true)
            public String call() {
                String sql = "select c.hypervisorType" +
                        " from ClusterVO c, PrimaryStorageClusterRefVO ref" +
                        " where c.uuid = ref.clusterUuid" +
                        " and ref.primaryStorageUuid = :psUuid";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("psUuid", psUuid);
                List<String> types = q.getResultList();
                return types.isEmpty() ? null : types.get(0);
            }
        }.call();

        if (type != null) {
            return HypervisorType.valueOf(type);
        }

        throw new OperationFailureException(operr("cannot find proper hypervisorType for primary storage[uuid:%s] to handle image format or volume format[%s]", psUuid, imageFormat));
    }

    @Override
    public void beforeCommitVolume(KVMHostInventory host, CommitVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockCommitCmd cmd, Completion completion) {
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(host.getHypervisorType());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.beforeBlockCommit(host, msg, cmd, completion);
    }

    @Override
    public void afterCommitVolume(KVMHostInventory host, CommitVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockCommitCmd cmd, CommitVolumeSnapshotOnHypervisorReply reply, Completion completion) {
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(host.getHypervisorType());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.afterBlockCommit(host, msg, cmd, reply, completion);
    }

    @Override
    public void failedToCommitVolume(KVMHostInventory host, CommitVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockCommitCmd cmd, KVMAgentCommands.BlockCommitResponse rsp, ErrorCode err) {
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(host.getHypervisorType());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.afterBlockCommitFailed(host, msg, cmd, rsp, err);
    }

    @Override
    public void beforePullVolume(KVMHostInventory host, PullVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockPullCmd cmd, Completion completion) {
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(host.getHypervisorType());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.beforeBlockPull(host, msg, cmd, completion);
    }

    @Override
    public void afterPullVolume(KVMHostInventory host, PullVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockPullCmd cmd, PullVolumeSnapshotOnHypervisorReply reply, Completion completion) {
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(host.getHypervisorType());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.afterBlockPull(host, msg, cmd, reply, completion);
    }

    @Override
    public void failedToPullVolume(KVMHostInventory host, PullVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockPullCmd cmd, KVMAgentCommands.BlockPullResponse rsp, ErrorCode err) {
        SharedBlockHypervisorFactory f = getHypervisorFactoryByHypervisorType(host.getHypervisorType());
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.afterBlockPullFailed(host, msg, cmd, rsp, err);
    }

    // This operation is triggered directly via API, no need to consider SHARED_BLOCK_TAKEOVER tag
    @Override
    protected void handle(APITakeoverPrimaryStorageMsg msg) {
        APITakeoverPrimaryStorageEvent event = new APITakeoverPrimaryStorageEvent(msg.getId());

        if (self.getStatus() != PrimaryStorageStatus.Disconnected) {
            event.setError(operr("cannot takeover primary storage[uuid:%s] in status[%s], expected: Disconnected",
                    self.getUuid(), self.getStatus()));
            bus.publish(event);
            return;
        }
        if (self.getState() == PrimaryStorageState.Maintenance) {
            event.setError(operr("cannot takeover primary storage[uuid:%s] in state[%s]",
                    self.getUuid(), self.getState()));
            bus.publish(event);
            return;
        }

        List<String> clusterUuids = self.getAttachedClusterRefs().stream().map(PrimaryStorageClusterRefVO::getClusterUuid).collect(Collectors.toList());
        if (clusterUuids.isEmpty()) {
            event.setError(operr("the shared block group primary storage[uuid:%s, name:%s] " +
                    "has not attached to any clusters", self.getUuid(), self.getName()));
            bus.publish(event);
            return;
        }

        List<String> candidateHostUuids = getCandidateHostUuidsForTakeover(clusterUuids);
        if (candidateHostUuids.isEmpty()) {
            event.setError(operr("no connected host available for takeover of primary storage[uuid:%s]", self.getUuid()));
            bus.publish(event);
            return;
        }

        SharedBlockHypervisorFactory f = getHypervisorFactoryByClusterUuid(clusterUuids.get(0));
        SharedBlockHypervisorBackend bkd = f.getHypervisorBackend(self);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("takeover-primary-storage-%s", self.getUuid()));
        chain.setData(new HashMap<>());

        chain.then(new NoRollbackFlow() {
            String __name__ = "check-primary-storage-consistency";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.checkPrimaryStorageConsistency(new ReturnValueCompletion<ConsistencyCheckResult>(trigger) {
                    @Override
                    public void success(ConsistencyCheckResult result) {
                        if (result.isConsistent()) {
                            trigger.fail(err(PrimaryStorageErrors.TAKEN_OVER,
                                    "primary storage[uuid:%s] VG UUID is already consistent with the database, takeover is not needed", self.getUuid()));
                            return;
                        }

                        if (!result.isSharedBlockGroupFound()) {
                            trigger.fail(operr("no VG with matching WWID set found on any host, " +
                                    "cannot takeover primary storage[uuid:%s]", self.getUuid()));
                            return;
                        }

                        data.put("consistencyCheckResult", result);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(operr("failed to check consistency before takeover for primary storage[uuid:%s]: %s",
                                self.getUuid(), errorCode));
                    }
                });
            }
        });

        chain.then(new NoRollbackFlow() {
            String __name__ = "takeover-on-candidate-hosts";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (msg.isDryRun()) {
                    trigger.next();
                    return;
                }

                tryTakeoverOnNextHost(candidateHostUuids.iterator(), bkd, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                }, new ArrayList<>());
            }
        });

        chain.then(new NoRollbackFlow() {
            String __name__ = "reconnect-after-takeover";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (msg.isDryRun()) {
                    trigger.next();
                    return;
                }

                ReconnectPrimaryStorageMsg rmsg = new ReconnectPrimaryStorageMsg();
                rmsg.setPrimaryStorageUuid(self.getUuid());
                bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, self.getUuid());
                bus.send(rmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                        } else {
                            // Takeover has already irreversibly modified storage (VG rename, PV UUID reset, sanlock reset).
                            // Reconnect failure is non-fatal — caller checks inventory.status to detect it.
                            logger.warn(String.format("reconnect after takeover failed for ps[uuid:%s]: %s",
                                    self.getUuid(), reply.getError()));
                            trigger.next();
                        }
                    }
                });
            }
        });

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                self = dbf.reload(self);
                event.setInventory(getSelfInventory());
                bus.publish(event);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                event.setError(errCode);
                bus.publish(event);
            }
        }).start();
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
                try {
                    SharedBlockHypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getRootVolumeUuid());
                    bkd.handle(msg, new ReturnValueCompletion<UpdateVmInstanceMetadataOnPrimaryStorageReply>(msg, chain) {
                        @Override
                        public void success(UpdateVmInstanceMetadataOnPrimaryStorageReply returnValue) {
                            bus.reply(msg, returnValue);
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            UpdateVmInstanceMetadataOnPrimaryStorageReply reply = new UpdateVmInstanceMetadataOnPrimaryStorageReply();
                            reply.setError(errorCode);
                            bus.reply(msg, reply);
                            chain.next();
                        }
                    });
                } catch (Exception e) {
                    UpdateVmInstanceMetadataOnPrimaryStorageReply reply = new UpdateVmInstanceMetadataOnPrimaryStorageReply();
                    reply.setError(Platform.operr("failed to update vm metadata on shared block primary storage[uuid:%s]: %s",
                            self.getUuid(), e.getMessage()));
                    bus.reply(msg, reply);
                    chain.next();
                }
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
        SharedBlockHypervisorBackend bkd = null;
        if (msg.getRootVolumeUuid() != null) {
            try {
                bkd = getHypervisorBackendByVolumeUuid(msg.getRootVolumeUuid());
            } catch (CloudRuntimeException e) {
                logger.warn(String.format("failed to get hypervisor backend by volume[uuid:%s], will try cluster fallback",
                        msg.getRootVolumeUuid()), e);
            }
        }
        if (bkd == null) {
            List<PrimaryStorageClusterRefVO> refs = Q.New(PrimaryStorageClusterRefVO.class)
                    .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, self.getUuid())
                    .list();
            if (refs.isEmpty()) {
                reply.setError(operr("no cluster attached to shared block primary storage[uuid:%s]", self.getUuid()));
                bus.reply(msg, reply);
                return;
            }

            bkd = getHypervisorFactoryByClusterUuid(refs.get(0).getClusterUuid()).getHypervisorBackend(self);
        }
        bkd.handle(msg, new ReturnValueCompletion<GetVmInstanceMetadataFromPrimaryStorageReply>(msg) {
            @Override
            public void success(GetVmInstanceMetadataFromPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
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

        List<PrimaryStorageClusterRefVO> refs = Q.New(PrimaryStorageClusterRefVO.class)
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, self.getUuid())
                .list();
        if (refs.isEmpty()) {
            reply.setError(operr("no cluster attached to shared block primary storage[uuid:%s]", self.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        SharedBlockHypervisorBackend bkd = getHypervisorFactoryByClusterUuid(refs.get(0).getClusterUuid()).getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<ScanVmInstanceMetadataFromPrimaryStorageReply>(msg) {
            @Override
            public void success(ScanVmInstanceMetadataFromPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
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
        SharedBlockHypervisorBackend bkd = null;
        if (msg.getRootVolumeUuid() != null) {
            try {
                bkd = getHypervisorBackendByVolumeUuid(msg.getRootVolumeUuid());
            } catch (CloudRuntimeException e) {
                logger.warn(String.format("failed to get hypervisor backend by volume[uuid:%s], will try cluster fallback",
                        msg.getRootVolumeUuid()), e);
            }
        }
        if (bkd == null) {
            List<PrimaryStorageClusterRefVO> refs = Q.New(PrimaryStorageClusterRefVO.class)
                    .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, self.getUuid())
                    .list();
            if (refs.isEmpty()) {
                reply.setError(operr("no cluster attached to shared block primary storage[uuid:%s]", self.getUuid()));
                bus.reply(msg, reply);
                return;
            }

            bkd = getHypervisorFactoryByClusterUuid(refs.get(0).getClusterUuid()).getHypervisorBackend(self);
        }
        bkd.handle(msg, new ReturnValueCompletion<CleanupVmInstanceMetadataOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CleanupVmInstanceMetadataOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
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

        List<PrimaryStorageClusterRefVO> refs = Q.New(PrimaryStorageClusterRefVO.class)
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, self.getUuid())
                .list();
        if (refs.isEmpty()) {
            reply.setError(operr("no cluster attached to shared block primary storage[uuid:%s]", self.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        SharedBlockHypervisorBackend bkd = getHypervisorFactoryByClusterUuid(refs.get(0).getClusterUuid()).getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<RebaseVolumeBackingFileOnPrimaryStorageReply>(msg) {
            @Override
            public void success(RebaseVolumeBackingFileOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }
}

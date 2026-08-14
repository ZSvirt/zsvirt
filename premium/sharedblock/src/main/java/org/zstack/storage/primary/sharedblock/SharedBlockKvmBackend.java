package org.zstack.storage.primary.sharedblock;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.ImageBackupStorageSelector;
import org.zstack.compute.vm.IsoOperator;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.gc.GarbageCollector;
import org.zstack.core.gc.GarbageCollectorVO;
import org.zstack.core.gc.GarbageCollectorVO_;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SingleFlightTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.ha.HaConstants;
import org.zstack.header.Component;
import org.zstack.header.cluster.ClusterConnectionStatus;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.vm.*;
import org.zstack.header.vm.metadata.*;
import org.zstack.header.volume.*;
import org.zstack.kvm.*;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO_;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.migration.VolumeMigrateExtensionPoint;
import org.zstack.storage.primary.*;
import org.zstack.storage.volume.VolumeErrors;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.Pair;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.*;
import static org.zstack.storage.primary.sharedblock.SharedBlockConstants.*;
import static org.zstack.storage.primary.sharedblock.SharedBlockKvmCommands.*;
import static org.zstack.storage.primary.sharedblock.SharedBlockSystemTags.THIN_PROVISIONING_INITIALIZE_SIZE_TAG;
import static org.zstack.storage.primary.sharedblock.SharedBlockSystemTags.THIN_PROVISIONING_INITIALIZE_SIZE_TAG_TOKEN;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class SharedBlockKvmBackend extends SharedBlockHypervisorBackend implements VmInstanceMigrateExtensionPoint,
        PreVmInstantiateResourceExtensionPoint, VmReleaseResourceExtensionPoint, ChangeVmImageExtensionPoint,
        VmAttachVolumeExtensionPoint, VmDetachVolumeExtensionPoint, KVMPreAttachIsoExtensionPoint, ResizeVolumeExtensionPoint,
        BeforeTakeLiveSnapshotsOnVolumes, VolumeSnapshotCreationExtensionPoint,
        VolumeMigrateExtensionPoint, VolumeSnapshotDeletionProtector, Component {
    private static final CLogger logger = Utils.getLogger(SharedBlockKvmBackend.class);

    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected SharedBlockGroupPrimaryStorageFactory primaryStorageFactory;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    private static ResourceConfigFacade srcf;
    private static final long MAX_ACTIVATE_VOLUME_GC_INTERVAL_IN_SEC = 600;
    public static final Map<String, String> activateVolumeGCs = new ConcurrentHashMap<>();
    public static final Map<String, Cache<Integer, Boolean>> exclusiveLockOpInPrimaryStorage = new HashMap<>();
    public static final Set<String> acquireExVgLockPaths = new HashSet<>();
    static {
        for (Field field : FieldUtils.getAllFields(SharedBlockKvmCommands.class)) {
            field.setAccessible(true);
            AcquireExVgLock at = field.getAnnotation(AcquireExVgLock.class);
            if (at != null) {
                try {
                    acquireExVgLockPaths.add((String) field.get(null));
                } catch (IllegalAccessException e) {
                    throw new CloudRuntimeException(e);
                }
            }
        }
    }

    public SharedBlockKvmBackend() {
    }

    public SharedBlockKvmBackend(PrimaryStorageVO self) {
        super(self);
    }

    private void connect(String hostUuid, String primaryStorageUuid, final ReturnValueCompletion<SharedBlockKvmCommands.ConnectRsp> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("connect-ps-%s-with-forcewipe", primaryStorageUuid);
            }

            @Override
            public void run(final SyncTaskChain chain) {
                boolean vgCreated = !SharedBlockSystemTags.SHARED_BLOCK_NOT_INITIALIZED.hasTag(primaryStorageUuid);

                if (!SharedBlockSystemTags.SHARED_BLOCK_FORCE_WIPE.hasTag(primaryStorageUuid) && vgCreated) {
                    doConnect(hostUuid, primaryStorageUuid, completion);
                    chain.next();
                } else {
                    doConnect(hostUuid, primaryStorageUuid, new ReturnValueCompletion<SharedBlockKvmCommands.ConnectRsp>(chain) {
                        @Override
                        public void success(SharedBlockKvmCommands.ConnectRsp connectRsp) {
                            completion.success(connectRsp);
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                            chain.next();
                        }
                    });
                }
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private List<String> getAllSharedBlockUuidsOnHost(String hostUuid) {
        FutureReturnValueCompletion completion = new FutureReturnValueCompletion(null);
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("get-sharedblock-uuids-on-host-%s", hostUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                completion.success(doGetAllSharedBlockUuidsOnHost(hostUuid));
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("get-sharedblock-uuids-on-host-%s", hostUuid);
            }
        });
        completion.await(TimeUnit.SECONDS.toMillis(30));  // wait for mount domain sync to dns
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
        return completion.getResult();
    }

    private List<String> doGetAllSharedBlockUuidsOnHost(String hostUuid) {
        List<String> result = new ArrayList<>();

        List<HostBlockDeviceForPrimaryStorageExtensionPoint> exts = pluginRgty.getExtensionList(HostBlockDeviceForPrimaryStorageExtensionPoint.class);
        for (HostBlockDeviceForPrimaryStorageExtensionPoint ext : exts) {
            result.addAll(ext.getBlockDevicesForPrimaryStorage(hostUuid));
        }

        String clusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid).eq(HostVO_.uuid, hostUuid).findValue();
        Set<String> primaryStorageUuids = new HashSet<>();

        if (!clusterUuid.isEmpty()) {
            primaryStorageUuids = new HashSet<>(SQL.New("select p.uuid from PrimaryStorageVO p, PrimaryStorageClusterRefVO r " +
                    "where p.uuid = r.primaryStorageUuid and r.clusterUuid = :cuuid and p.type = :ptype")
                    .param("cuuid", clusterUuid).param("ptype", SHARED_BLOCK_PRIMARY_STORAGE_TYPE).list());

        }

        String sql = "select primaryStorageUuid from SharedBlockGroupPrimaryStorageHostRefVO " +
                "where hostUuid =: hostUuid and createDate > :attachedTimeExceededApiTimeout";
        Set<String> uuidsFromHostRef = new HashSet<>(SQL.New(sql)
                .param("hostUuid", hostUuid)
                .param("attachedTimeExceededApiTimeout", new Timestamp(
                        System.currentTimeMillis() - timeoutMgr.getMessageTimeout(new APIAttachPrimaryStorageToClusterMsg())))
                .list());

        primaryStorageUuids.addAll(uuidsFromHostRef);

        if (!primaryStorageUuids.isEmpty()) {
            result.addAll(Q.New(SharedBlockVO.class).select(SharedBlockVO_.diskUuid)
                    .in(SharedBlockVO_.sharedBlockGroupUuid, primaryStorageUuids).listValues());
        }

        return result;
    }

    private void doConnect(String hostUuid, String primaryStorageUuid, final ReturnValueCompletion<SharedBlockKvmCommands.ConnectRsp> completion) {
        List<String> sharedBlockDiskUuids = Q.New(SharedBlockVO.class)
                .select(SharedBlockVO_.diskUuid)
                .eq(SharedBlockVO_.sharedBlockGroupUuid, self.getUuid())
                .listValues();
        SharedBlockKvmCommands.ConnectCmd cmd = new SharedBlockKvmCommands.ConnectCmd();
        cmd.vgUuid = primaryStorageUuid;
        cmd.hostId = new SharedBlockHostIdGetter().getHostIdRef(hostUuid, getSelfInventory().getUuid()).getHostId().toString();
        cmd.sharedBlockUuids = sharedBlockDiskUuids;
        cmd.hostUuid = hostUuid;
        cmd.allSharedBlockUuids = getAllSharedBlockUuidsOnHost(hostUuid);
        cmd.enableLvmetad = SharedBlockGlobalConfig.ENABLE_LVMETAD.value(Boolean.class);

        String clusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid).eq(HostVO_.uuid, hostUuid).findValue();
        cmd.ioTimeout = rcf.getResourceConfigValue(SharedBlockGlobalConfig.HEARTBEAT_IO_TIMEOUT, clusterUuid, Long.class);
        cmd.maxActualSizeFactor = SharedBlockGlobalConfig.MAX_ACTUAL_SIZE_FACTOR.value(Long.class);
        if (SharedBlockSystemTags.SHARED_BLOCK_FORCE_WIPE.hasTag(primaryStorageUuid)) {
            cmd.forceWipe = true;
        }
        cmd.isFirst = SharedBlockSystemTags.SHARED_BLOCK_NOT_INITIALIZED.hasTag(primaryStorageUuid);

        httpCall(SharedBlockKvmCommands.CONNECT_PATH, hostUuid, cmd, true, SharedBlockKvmCommands.ConnectRsp.class, new ReturnValueCompletion<SharedBlockKvmCommands.ConnectRsp>(completion) {
            @Override
            public void success(SharedBlockKvmCommands.ConnectRsp rsp) {
                changeSharedBlockGroupPrimaryStorageHostRefStatus(primaryStorageUuid, hostUuid, PrimaryStorageHostStatus.Connected);

                if (SharedBlockSystemTags.SHARED_BLOCK_FORCE_WIPE.hasTag(primaryStorageUuid)) {
                    SharedBlockSystemTags.SHARED_BLOCK_FORCE_WIPE.delete(primaryStorageUuid);
                }

                if (SharedBlockSystemTags.SHARED_BLOCK_NOT_INITIALIZED.hasTag(primaryStorageUuid)) {
                    SharedBlockSystemTags.SHARED_BLOCK_NOT_INITIALIZED.delete(primaryStorageUuid);
                }

                if (rsp.hostId != null && !rsp.hostId.equals(new SharedBlockHostIdGetter().getHostIdRef(hostUuid, primaryStorageUuid).getHostId().toString())) {
                    logger.warn(String.format("host id[id: %s] in host[uuid: %s] for shared block group primary storage[uuid %s]" +
                            "is different to host id in database[id: %s], update db to agent value[id: %s]",
                            rsp.hostId, hostUuid, primaryStorageUuid, cmd.hostId, rsp.hostId));
                    new SharedBlockHostIdGetter().allocateAndSetHostId(hostUuid, primaryStorageUuid, Integer.parseInt(rsp.hostId), PrimaryStorageHostStatus.Connected);
                }
                completion.success(rsp);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                changeSharedBlockGroupPrimaryStorageHostRefStatus(primaryStorageUuid, hostUuid, PrimaryStorageHostStatus.Disconnected);
                completion.fail(errorCode);
            }
        });
    }

    private void changeSharedBlockGroupPrimaryStorageHostRefStatus(String primaryStorageUuid, String hostUuid, PrimaryStorageHostStatus status) {
        SharedBlockGroupPrimaryStorageHostRefVO vo = Q.New(SharedBlockGroupPrimaryStorageHostRefVO.class)
                .eq(SharedBlockGroupPrimaryStorageHostRefVO_.hostUuid, hostUuid)
                .eq(SharedBlockGroupPrimaryStorageHostRefVO_.primaryStorageUuid, primaryStorageUuid)
                .find();
        if (vo == null) {
            logger.warn(String.format("can not find ref of host uuid %s and ps %s", hostUuid, primaryStorageUuid));
            vo = new SharedBlockHostIdGetter().getHostIdRef(hostUuid, primaryStorageUuid);
        }

        PrimaryStorageHostStatus oldStatus = vo.getStatus();
        vo.setStatus(status);
        vo = dbf.updateAndRefresh(vo);

        if (!oldStatus.equals(vo.getStatus())) {
            PrimaryStorageCanonicalEvent.PrimaryStorageHostStatusChangeData data = new PrimaryStorageCanonicalEvent.PrimaryStorageHostStatusChangeData();
            data.setHostUuid(hostUuid);
            data.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
            data.setOldStatus(oldStatus);
            data.setNewStatus(vo.getStatus());
            evtf.fire(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_HOST_STATUS_CHANGED_PATH, data);
        }
    }

    private void disconnect(String hostUuid, final Completion completion) {
        disconnect(hostUuid, self.getUuid(), false, completion);
    }

    private void disconnect(String hostUuid, String primaryStorageUuid, boolean stopServices, final Completion completion) {
        SharedBlockKvmCommands.DisonnectCmd cmd = new SharedBlockKvmCommands.DisonnectCmd();
        cmd.vgUuid = primaryStorageUuid;
        cmd.hostUuid = hostUuid;
        cmd.stopServices = stopServices;

        httpCall(SharedBlockKvmCommands.DISCONNECT_PATH, hostUuid, cmd, true, SharedBlockKvmCommands.AgentRsp.class, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(completion) {
            @Override
            public void success(SharedBlockKvmCommands.AgentRsp rsp) {
                SharedBlockGroupPrimaryStorageHostRefVO vo = Q.New(SharedBlockGroupPrimaryStorageHostRefVO.class)
                        .eq(SharedBlockGroupPrimaryStorageHostRefVO_.hostUuid, hostUuid)
                        .eq(SharedBlockGroupPrimaryStorageHostRefVO_.primaryStorageUuid, self.getUuid())
                        .find();
                if (vo != null) {
                    dbf.remove(vo);
                }

                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void connect(String hostUuid, final ReturnValueCompletion<SharedBlockKvmCommands.ConnectRsp> completion) {
        connect(hostUuid, self.getUuid(), completion);
    }

    @Override
    void takeover(String hostUuid, final ReturnValueCompletion<TakeoverRsp> completion) {
        String primaryStorageUuid = self.getUuid();
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("takeover-ps-%s", primaryStorageUuid);
            }

            @Override
            public void run(final SyncTaskChain chain) {
                List<String> sharedBlockDiskUuids = Q.New(SharedBlockVO.class).select(SharedBlockVO_.diskUuid)
                        .eq(SharedBlockVO_.sharedBlockGroupUuid, primaryStorageUuid).listValues();
                TakeoverCmd cmd = new TakeoverCmd();
                cmd.vgUuid = primaryStorageUuid;
                cmd.hostId = new SharedBlockHostIdGetter().getHostIdRef(hostUuid, getSelfInventory().getUuid()).getHostId().toString();
                cmd.sharedBlockUuids = sharedBlockDiskUuids;
                cmd.hostUuid = hostUuid;
                cmd.allSharedBlockUuids = getAllSharedBlockUuidsOnHost(hostUuid);
                cmd.enableLvmetad = SharedBlockGlobalConfig.ENABLE_LVMETAD.value(Boolean.class);
                String clusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid).eq(HostVO_.uuid, hostUuid).findValue();
                cmd.ioTimeout = rcf.getResourceConfigValue(SharedBlockGlobalConfig.HEARTBEAT_IO_TIMEOUT, clusterUuid, Long.class);
                httpCall(SharedBlockKvmCommands.TAKEOVER_PATH, hostUuid, cmd, true, TakeoverRsp.class, new ReturnValueCompletion<TakeoverRsp>(chain) {
                    @Override
                    public void success(TakeoverRsp rsp) {
                        completion.success(rsp);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    /*
     * c.f. ZSTAC-40811, and confluence pageId=102074708
     *
     * To deal with LUN assignment change, we need to physically detach all disks in
     * current SBLK in `detachHook()', for *all* hosts that are up and running.
     *
     * For `attachHook()', a rescan is needed for *all* hosts that are up and running.
     */
    @Override
    public void attachHook(final String clusterUuid, final Completion completion) {
        connectByClusterUuid(clusterUuid, true, new ReturnValueCompletion<ClusterConnectionStatus>(completion) {
            @Override
            public void success(ClusterConnectionStatus clusterStatus) {
                if (clusterStatus == ClusterConnectionStatus.PartiallyConnected || clusterStatus == ClusterConnectionStatus.FullyConnected){
                    changeStatus(PrimaryStorageStatus.Connected);
                } else if (self.getStatus() == PrimaryStorageStatus.Disconnected && clusterStatus == ClusterConnectionStatus.Disconnected){
                    hookToKVMHostConnectedEventToChangeStatusToConnected();
                }
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                List<PrimaryStorageHostRefVO> refs = SQL.New("select r from PrimaryStorageHostRefVO r, HostVO h " +
                        "where r.hostUuid = h.uuid " +
                        "and h.clusterUuid = :cuuid " +
                        "and r.primaryStorageUuid = :puuid")
                        .param("cuuid", clusterUuid)
                        .param("puuid", self.getUuid())
                        .list();
                dbf.removeCollection(refs, PrimaryStorageHostRefVO.class);
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void detachHook(final String clusterUuid, final Completion completion) {
        disconnectByClusterUuid(clusterUuid, new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void addSharedBlockToSharedBlockGroup(String diskUuid, String sharedBlockGroupUuid, Completion completion) {
        List<PrimaryStorageClusterRefVO> refVOS = Q.New(PrimaryStorageClusterRefVO.class).eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, sharedBlockGroupUuid).list();
        if (refVOS == null || refVOS.isEmpty()) {
            completion.success();
            return;
        }

        if (sharedBlockGroupFactory.getConnectedHostsForOperation(sharedBlockGroupUuid) == null ||
                sharedBlockGroupFactory.getConnectedHostsForOperation(sharedBlockGroupUuid).isEmpty()) {
            completion.success();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("add-shared-block-%s-to-group%s", diskUuid, sharedBlockGroupUuid));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                ErrorCodeList errList = new ErrorCodeList();
                new While<>(refVOS).step((ref, completion1) -> {
                    doCheckByClusterUuid(ref.getClusterUuid(), sharedBlockGroupUuid, diskUuid, new Completion(completion1) {
                        @Override
                        public void success() {
                            completion1.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            errList.getCauses().add(errorCode);
                            completion1.done();
                        }
                    });
                }, 10).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errList.getCauses().isEmpty()) {
                            trigger.next();
                        } else {
                            trigger.fail(errList.getCauses().get(0));
                        }
                    }
                });
            }
        }).then(new Flow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                doConfigFilter(refVOS.stream().map(PrimaryStorageClusterRefVO::getClusterUuid).collect(Collectors.toList()),
                        sharedBlockGroupUuid, new ArrayList<>(), new Completion(trigger) {
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

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                doConfigFilter(refVOS.stream().map(PrimaryStorageClusterRefVO::getClusterUuid).collect(Collectors.toList()),
                        sharedBlockGroupUuid, Collections.singletonList(diskUuid), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                HostInventory hostInventory = sharedBlockGroupFactory.getConnectedHostsForOperation(sharedBlockGroupUuid).get(0);
                SharedBlockKvmCommands.AddDiskCmd cmd = new SharedBlockKvmCommands.AddDiskCmd();
                cmd.diskUuid = diskUuid;
                cmd.vgUuid = sharedBlockGroupUuid;
                cmd.hostUuid = hostInventory.getUuid();
                cmd.allSharedBlockUuids = getAllSharedBlockUuidsOnHost(cmd.hostUuid);
                cmd.forceWipe = SharedBlockSystemTags.SHARED_BLOCK_FORCE_WIPE.hasTag(sharedBlockGroupUuid);

                httpCall(SharedBlockKvmCommands.ADD_SHARED_BLOCK, hostInventory.getUuid(), cmd, true, SharedBlockKvmCommands.AgentRsp.class, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(trigger) {
                    @Override
                    public void success(SharedBlockKvmCommands.AgentRsp rsp) {
                        changeSharedBlockGroupPrimaryStorageHostRefStatus(self.getUuid(), hostInventory.getUuid(), PrimaryStorageHostStatus.Connected);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    public String makeInstallPath(String vgUuid, String volUuid) {
        return String.format("%s%s/%s",
                SHARED_BLOCK_INSTALL_PATH_SCHEME, vgUuid, volUuid);
    }

    public String makeInstallPath(String volUuid) {
        return String.format("%s%s/%s",
                SHARED_BLOCK_INSTALL_PATH_SCHEME, getSelfInventory().getUuid(), volUuid);
    }

    public String makeRootVolumeInstallUrl(VolumeInventory vol) {
        return makeInstallPath(vol.getUuid());
    }

    public String makeDataVolumeInstallUrl(String volUuid) {
        return makeInstallPath(volUuid);
    }

    public String makeCachedImageInstallUrl(ImageInventory iminv) {
        return ImageCacheUtil.getImageCachePath(iminv, this::makeInstallPath);
    }

    public String makeCachedImageInstallUrlFromImageUuidForTemplate(String imageUuid) {
        return makeInstallPath(imageUuid);
    }

    public String makeTemplateFromVolumeInWorkspacePath(String imageUuid) {
        return makeInstallPath(imageUuid);
    }

    public String makeSnapshotInstallPath(VolumeInventory vol, VolumeSnapshotInventory snapshot) {
        return makeInstallPath(snapshot.getUuid());
    }

    @Override
    void handle(InstantiateVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion) {
        if (msg instanceof InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) {
            createRootVolume((InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) msg, completion);
        } else if (msg instanceof InstantiateMemoryVolumeOnPrimaryStorageMsg) {
            createMemoryVolume((InstantiateMemoryVolumeOnPrimaryStorageMsg) msg, completion);
        } else {
            createEmptyVolume(msg, completion);
        }
    }

    private void createMemoryVolume(InstantiateMemoryVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion) {
        InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
        VolumeInventory volume = new VolumeInventory();
        volume.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
        volume.setInstallPath("");
        reply.setVolume(volume);
        completion.success(reply);
    }

    @Override
    void handle(DownloadVolumeTemplateToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadVolumeTemplateToPrimaryStorageReply> completion) {
        DownloadVolumeTemplateToPrimaryStorageReply reply = new DownloadVolumeTemplateToPrimaryStorageReply();
        VmInstanceSpec.ImageSpec ispec = msg.getTemplateSpec();

        ImageCache cache = new ImageCache();
        cache.primaryStorageInstallPath = makeCachedImageInstallUrl(ispec.getInventory());
        cache.image = ispec.getInventory();
        if (ispec.getSelectedBackupStorage() != null) {
            cache.backupStorageUuid = ispec.getSelectedBackupStorage().getBackupStorageUuid();
            cache.backupStorageInstallPath = ispec.getSelectedBackupStorage().getInstallPath();
        }
        cache.download(new ReturnValueCompletion<ImageCacheInventory>(completion) {
            @Override
            public void success(ImageCacheInventory returnValue) {
                reply.setImageCache(returnValue);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void createEmptyVolumeByInstallPath(String installPath, Long size, String volumeUuid, String hostUuid, Completion completion) {
        final SharedBlockKvmCommands.CreateEmptyVolumeCmd cmd = new SharedBlockKvmCommands.CreateEmptyVolumeCmd();
        cmd.installPath = installPath;
        cmd.name = installPath;
        cmd.size = size;
        cmd.volumeUuid = volumeUuid;
        cmd.vgUuid = getPrimaryStorageUuidByInstallPath(installPath);

        new Do(hostUuid).go(SharedBlockKvmCommands.CREATE_EMPTY_VOLUME_PATH, cmd, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(completion) {
            @Override
            public void success(SharedBlockKvmCommands.AgentRsp returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private static String getDataVolumeFormat(final VolumeInventory volume) {
        if (!volume.isShareable()) {
            return VolumeConstant.VOLUME_FORMAT_QCOW2;
        }

        return VolumeConstant.VOLUME_FORMAT_RAW;
    }

    private void createEmptyVolume(InstantiateVolumeOnPrimaryStorageMsg msg, final ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion) {
        VolumeInventory volume = msg.getVolume();
        String hostUuid = msg.getDestHost().getUuid();
        List<String> systemTags = msg.getSystemTags();
        if (volume.isShareable() && getSharedVolumeProvisioning(volume)
                .equals(VolumeProvisioningStrategy.ThinProvisioning.toString())) {
            completion.fail(Platform.operr("shared volume not support thin provisioning"));
            return;
        }

        final SharedBlockKvmCommands.CreateEmptyVolumeCmd cmd = new SharedBlockKvmCommands.CreateEmptyVolumeCmd();
        cmd.installPath = VolumeType.Root.toString().equals(volume.getType()) ?
                makeRootVolumeInstallUrl(volume) :
                makeDataVolumeInstallUrl(volume.getUuid()); // include NvRam
        cmd.name = volume.getName();
        cmd.size = volume.getSize();
        cmd.volumeUuid = volume.getUuid();
        cmd.volumeFormat = getDataVolumeFormat(volume);
        if (!CollectionUtils.isEmpty(systemTags) && systemTags.contains(VolumeSystemTags.NO_ZERO_FILLED_VOLUME.getTagFormat())) {
            cmd.zeroFilled = false;
        }

        if (!CollectionUtils.isEmpty(msg.getSystemTags())) {
            String matchedTag = CollectionUtils.findOneOrNull(msg.getSystemTags(), THIN_PROVISIONING_INITIALIZE_SIZE_TAG::isMatch);
            long thinProvisioningInitializeSize = matchedTag == null ? 0 :
                    Long.parseLong(THIN_PROVISIONING_INITIALIZE_SIZE_TAG.getTokenByTag(matchedTag, THIN_PROVISIONING_INITIALIZE_SIZE_TAG_TOKEN));
            if (thinProvisioningInitializeSize >= MIN_THIN_PROVISIONING_INITIALIZE_SIZE) {
                cmd.getAddons().put(THIN_PROVISIONING_INITIALIZE_SIZE_TAG_TOKEN, Math.min(thinProvisioningInitializeSize, volume.getSize()));
            }
        }

        new Do(hostUuid).go(SharedBlockKvmCommands.CREATE_EMPTY_VOLUME_PATH, cmd, CreateEmptyVolumeRsp.class,
                new ReturnValueCompletion<SharedBlockKvmCommands.CreateEmptyVolumeRsp>(completion) {
            @Override
            public void success(SharedBlockKvmCommands.CreateEmptyVolumeRsp returnValue) {
                InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
                volume.setInstallPath(cmd.installPath);
                volume.setFormat(cmd.volumeFormat);
                volume.setActualSize(returnValue.actualSize);
                if (returnValue.size != null) {
                    volume.setSize(returnValue.size);
                }

                reply.setVolume(volume);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void createRootVolume(InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg msg, final ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion) {
        final VmInstanceSpec.ImageSpec ispec = msg.getTemplateSpec();
        final ImageInventory image = ispec.getInventory();

        if (!ImageConstant.ImageMediaType.RootVolumeTemplate.toString().equals(image.getMediaType())) {
            createEmptyVolume(msg, completion);
            return;
        }

        final VolumeInventory volume = msg.getVolume();
        final String hostUuid = msg.getDestHost().getUuid();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("kvm-shared-block-storage-create-root-volume-from-image-%s", image.getUuid()));
        chain.then(new ShareFlow() {
            final String pathInCache = makeCachedImageInstallUrl(image);
            String installPath;
            Long actualSize;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        DownloadVolumeTemplateToPrimaryStorageMsg dmsg = new DownloadVolumeTemplateToPrimaryStorageMsg();
                        dmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                        dmsg.setHostUuid(msg.getDestHost().getUuid());
                        dmsg.setTemplateSpec(msg.getTemplateSpec());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, dmsg.getPrimaryStorageUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
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

                flow(new NoRollbackFlow() {
                    String __name__ = "create-volume-from-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        installPath = makeRootVolumeInstallUrl(volume);

                        SharedBlockKvmCommands.CreateVolumeFromCacheCmd cmd = new SharedBlockKvmCommands.CreateVolumeFromCacheCmd();
                        cmd.installPath = installPath;
                        cmd.templatePathInCache = pathInCache;
                        cmd.volumeUuid = volume.getUuid();
                        cmd.vgUuid = msg.getPrimaryStorageUuid();
                        cmd.hostUuid = hostUuid;
                        if (image.getSize() < volume.getSize()) {
                            cmd.virtualSize = volume.getSize();
                        }

                        if (!CollectionUtils.isEmpty(msg.getSystemTags())) {
                            String matchedTag = CollectionUtils.findOneOrNull(msg.getSystemTags(), THIN_PROVISIONING_INITIALIZE_SIZE_TAG::isMatch);
                            long thinProvisioningInitializeSize = matchedTag == null ? 0 :
                                    Long.parseLong(THIN_PROVISIONING_INITIALIZE_SIZE_TAG.getTokenByTag(matchedTag,
                                            THIN_PROVISIONING_INITIALIZE_SIZE_TAG_TOKEN));
                            if (thinProvisioningInitializeSize >= MIN_THIN_PROVISIONING_INITIALIZE_SIZE) {
                                cmd.getAddons().put(THIN_PROVISIONING_INITIALIZE_SIZE_TAG_TOKEN,
                                        Math.min(thinProvisioningInitializeSize, volume.getSize()));
                            }
                        }

                        httpCall(SharedBlockKvmCommands.CREATE_VOLUME_FROM_CACHE_PATH, hostUuid, cmd, SharedBlockKvmCommands.CreateVolumeFromCacheRsp.class,
                                new ReturnValueCompletion<SharedBlockKvmCommands.CreateVolumeFromCacheRsp>(trigger) {
                                    @Override
                                    public void success(SharedBlockKvmCommands.CreateVolumeFromCacheRsp rsp) {
                                        actualSize = rsp.actualSize;
                                        if (rsp.size != null) {
                                            volume.setSize(rsp.size);
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

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
                        volume.setInstallPath(installPath);
                        volume.setFormat(getDataVolumeFormat(volume));
                        volume.setActualSize(actualSize);
                        reply.setVolume(volume);
                        completion.success(reply);
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    void
    handle(DeleteVolumeOnPrimaryStorageMsg msg, final ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply> completion) {
        deleteBits(msg.getVolume().getInstallPath(), new Completion(completion) {
            @Override
            public void success() {
                DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private String getSharedVolumeProvisioning(VolumeInventory volume) {
        if (VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.hasTag(volume.getUuid(), VolumeVO.class)) {
            return VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.getTokenByResourceUuid(volume.getUuid(), VolumeVO.class, VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN);
        }

        SystemTagCreator tagCreator = VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.newSystemTagCreator(volume.getUuid());
        tagCreator.setTagByTokens(
                map(
                        e(VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN,
                                VolumeProvisioningStrategy.ThickProvisioning.toString())
                )
        );
        tagCreator.inherent = false;
        tagCreator.create();

        return VolumeProvisioningStrategy.ThickProvisioning.toString();
    }

    @Override
    void handle(final DownloadDataVolumeToPrimaryStorageMsg msg, final ReturnValueCompletion<DownloadDataVolumeToPrimaryStorageReply> completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("download-data-volume-%s-to-primary-storage", msg.getVolumeUuid()));
        chain.then(new ShareFlow() {
            final DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();
            final String installPath = makeDataVolumeInstallUrl(msg.getVolumeUuid());
            final VolumeVO volume = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);

            @Override
            public void setup() {
                String __name__ = String.format("download-bits-for-volume-%s", msg.getVolumeUuid());

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (volume.isShareable() && getSharedVolumeProvisioning(VolumeInventory.valueOf(volume))
                                .equals(VolumeProvisioningStrategy.ThinProvisioning.toString())) {
                            trigger.fail(Platform.operr("shared volume not support thin provisioning"));
                            return;
                        }

                        BackupStorageSharedBlockKvmDownloader downloader = getBackupStorageKvmDownloader(msg.getBackupStorageRef().getBackupStorageUuid());
                        downloader.downloadBits(msg.getBackupStorageRef().getInstallPath(), installPath, LvmlockdLockingType.NULL, new Completion(trigger) {
                            @Override
                            public void success() {
                                reply.setFormat(msg.getImage().getFormat());
                                reply.setInstallPath(installPath);
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
                    String __name__ = String.format("do-resize-volume-%s", installPath);

                    @Override
                    public void run(FlowTrigger trigger, Map map) {
                        SharedBlockKvmCommands.ResizeVolumeCmd cmd = new SharedBlockKvmCommands.ResizeVolumeCmd();
                        cmd.installPath = installPath;
                        cmd.live = false;
                        cmd.size = msg.getImage().getSize();
                        cmd.vgUuid = getSelfInventory().getUuid();
                        cmd.volumeUuid = msg.getVolumeUuid();

                        new KvmAgentCommandDispatcher(getSelfInventory().getUuid()).go(RESIZE_VOLUME_PATH, cmd, SharedBlockKvmCommands.AgentRsp.class, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(trigger) {
                            @Override
                            public void success(SharedBlockKvmCommands.AgentRsp returnValue) {
                                if (returnValue.success) {
                                    trigger.next();
                                    return;
                                }

                                trigger.fail(operr("%s", returnValue.error));
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = String.format("convert-volume-format-%s", installPath);

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (!volume.isShareable()) {
                            trigger.next();
                            return;
                        }

                        SharedBlockKvmCommands.ConvertVolumeFormatCmd cmd = new SharedBlockKvmCommands.ConvertVolumeFormatCmd();
                        cmd.installPath = installPath;
                        cmd.dstFormat = ImageConstant.RAW_FORMAT_STRING;

                        new KvmAgentCommandDispatcher(getSelfInventory().getUuid()).go(CONVERT_VOLUME_FORMAT_PATH, cmd, SharedBlockKvmCommands.AgentRsp.class, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(trigger) {
                            @Override
                            public void success(SharedBlockKvmCommands.AgentRsp returnValue) {
                                if (returnValue.success) {
                                    reply.setFormat(ImageConstant.RAW_FORMAT_STRING);
                                    trigger.next();
                                    return;
                                }

                                trigger.fail(operr("%s", returnValue.error));
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = String.format("convert-to-volume-%s", installPath);

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SharedBlockKvmCommands.ConvertImageToVolumeCmd cmd = new SharedBlockKvmCommands.ConvertImageToVolumeCmd();
                        cmd.primaryStorageInstallPath = installPath;
                        cmd.vgUuid = getSelfInventory().getUuid();

                        new KvmAgentCommandDispatcher(getSelfInventory().getUuid()).go(CONVERT_IMAGE_TO_VOLUME, cmd, SharedBlockKvmCommands.AgentRsp.class, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(trigger) {
                            @Override
                            public void success(SharedBlockKvmCommands.AgentRsp returnValue) {
                                if (returnValue.success) {
                                    trigger.next();
                                    return;
                                }

                                trigger.fail(operr("%s", returnValue.error));
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success(reply);
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    void handle(GetInstallPathForDataVolumeDownloadMsg msg, ReturnValueCompletion<GetInstallPathForDataVolumeDownloadReply> completion) {
        final String installPath = makeDataVolumeInstallUrl(msg.getVolumeUuid());
        GetInstallPathForDataVolumeDownloadReply reply = new GetInstallPathForDataVolumeDownloadReply();
        reply.setInstallPath(installPath);
        completion.success(reply);
    }

    @Override
    void handle(DeleteVolumeBitsOnPrimaryStorageMsg msg, final ReturnValueCompletion<DeleteVolumeBitsOnPrimaryStorageReply> completion) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                deleteBits(msg.getInstallPath(), msg.isFolder(), new Completion(completion) {
                    @Override
                    public void success() {
                        DeleteVolumeBitsOnPrimaryStorageReply reply = new DeleteVolumeBitsOnPrimaryStorageReply();
                        completion.success(reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            public String getSyncSignature() {
                return String.format("delete-volume-bits-on-sblk-%s", msg.getPrimaryStorageUuid());
            }

            @Override
            public String getName() {
                return String.format("delete-path-%s-on-ps-%s", msg.getInstallPath(), msg.getPrimaryStorageUuid());
            }

            @Override
            public int getSyncLevel() {
                return 3;
            }
        });
    }

    @Override
    void handle(final DeleteBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteBitsOnPrimaryStorageReply> completion) {
        deleteBits(msg.getInstallPath(), msg.isFolder(), new Completion(completion) {
            @Override
            public void success() {
                DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(DownloadIsoToPrimaryStorageMsg msg, final ReturnValueCompletion<DownloadIsoToPrimaryStorageReply> completion) {
        VmInstanceSpec.ImageSpec ispec = msg.getIsoSpec();
        ImageCache cache = new ImageCache();

        cache.image = ispec.getInventory();
        cache.primaryStorageInstallPath = makeCachedImageInstallUrl(ispec.getInventory());
        cache.backupStorageUuid = ispec.getSelectedBackupStorage().getBackupStorageUuid();
        cache.backupStorageInstallPath = ispec.getSelectedBackupStorage().getInstallPath();
        cache.download(new ReturnValueCompletion<ImageCacheInventory>(completion) {
            @Override
            public void success(ImageCacheInventory returnValue) {
                DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
                reply.setInstallPath(returnValue.getInstallUrl());
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(DeleteIsoFromPrimaryStorageMsg msg, ReturnValueCompletion<DeleteIsoFromPrimaryStorageReply> completion) {
        // The ISO is in the image cache, no need to delete it
        DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
        completion.success(reply);
    }

    @Override
    void handle(CheckSnapshotMsg msg, Completion completion) {
        VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class));

        String hostUuid = primaryStorageFactory.getConnectedHostsForOperation(getSelfInventory(), vol).get(0).getUuid();
        if (vol.getVmInstanceUuid() != null){
            VmInstanceState state = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.state)
                    .eq(VmInstanceVO_.uuid, vol.getVmInstanceUuid())
                    .findValue();

            if (state != VmInstanceState.Running && state != VmInstanceState.Paused && state != VmInstanceState.Stopped){
                completion.fail(operr("vm[uuid:%s] is not Running, Paused or Stopped, current state[%s]",
                        vol.getVmInstanceUuid(), state));
                return;
            }
        }

        CheckSnapshotOnHypervisorMsg hmsg = new CheckSnapshotOnHypervisorMsg();
        hmsg.setHostUuid(hostUuid);
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
        bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(hmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply ret) {
                if (!ret.isSuccess()) {
                    completion.fail(ret.getError());
                    return;
                }

                completion.success();
            }
        });
    }

    private String getImageCacheInstallPath(String imageUuid) {
        return Q.New(ImageCacheVO.class)
                .select(ImageCacheVO_.installUrl)
                .eq(ImageCacheVO_.primaryStorageUuid, self.getUuid())
                .eq(ImageCacheVO_.imageUuid, imageUuid).findValue();
    }

    @Override
    void handle(TakeSnapshotMsg msg, final ReturnValueCompletion<TakeSnapshotReply> completion) {
        final VolumeSnapshotInventory sp = msg.getStruct().getCurrent();
        VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(sp.getVolumeUuid(), VolumeVO.class));

        String hostUuid = primaryStorageFactory.getConnectedHostsForOperation(getSelfInventory(), vol).get(0).getUuid();
        if (vol.getVmInstanceUuid() != null){
            VmInstanceState state = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.state)
                    .eq(VmInstanceVO_.uuid, vol.getVmInstanceUuid())
                    .findValue();

            if (state != VmInstanceState.Running && state != VmInstanceState.Paused && state != VmInstanceState.Stopped){
                completion.fail(operr("vm[uuid:%s] is not Running, Paused or Stopped, current state[%s]",
                        vol.getVmInstanceUuid(), state));
                return;
            }
        }


        TakeSnapshotOnHypervisorMsg hmsg = new TakeSnapshotOnHypervisorMsg();
        hmsg.setHostUuid(hostUuid);
        hmsg.setVmUuid(vol.getVmInstanceUuid());
        hmsg.setVolume(vol);
        hmsg.setSnapshotName(msg.getStruct().getCurrent().getUuid());
        hmsg.setFullSnapshot(msg.getStruct().isFullSnapshot());
        String installPath = makeSnapshotInstallPath(vol, sp);
        hmsg.setInstallPath(installPath);
        bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(hmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                TakeSnapshotOnHypervisorReply treply = (TakeSnapshotOnHypervisorReply) reply;
                sp.setSize(treply.getSize());
                sp.setPrimaryStorageUuid(self.getUuid());
                sp.setPrimaryStorageInstallPath(treply.getSnapshotInstallPath());
                sp.setType(VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString());

                TakeSnapshotReply ret = new TakeSnapshotReply();
                ret.setNewVolumeInstallPath(treply.getNewVolumeInstallPath());
                ret.setInventory(sp);

                completion.success(ret);
            }
        });
    }

    @Override
    void handle(DeleteSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply> completion) {
        deleteBits(msg.getSnapshot().getPrimaryStorageInstallPath(), new Completion(completion) {
            @Override
            public void success() {
                DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply> completion) {
        VolumeSnapshotInventory sp = msg.getSnapshot();
        SharedBlockKvmCommands.RevertVolumeFromSnapshotCmd cmd = new SharedBlockKvmCommands.RevertVolumeFromSnapshotCmd();
        cmd.snapshotInstallPath = sp.getPrimaryStorageInstallPath();
        cmd.volumeUuid = msg.getVolume().getUuid();
        cmd.installPath = String.format("%s%s/%s", SharedBlockConstants.SHARED_BLOCK_INSTALL_PATH_SCHEME, msg.getPrimaryStorageUuid(), getUuid());

        new Do(msg.getVolume()).go(REVERT_VOLUME_FROM_SNAPSHOT_PATH, cmd, RevertVolumeFromSnapshotRsp.class, new ReturnValueCompletion<RevertVolumeFromSnapshotRsp>(completion) {
            @Override
            public void success(RevertVolumeFromSnapshotRsp rsp) {
                RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
                reply.setNewVolumeInstallPath(convertAbsolutePathToInstall(rsp.newVolumeInstallPath));
                reply.setSize(rsp.size);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg, final ReturnValueCompletion<ReInitRootVolumeFromTemplateOnPrimaryStorageReply> completion) {
        SharedBlockKvmCommands.RevertVolumeFromSnapshotCmd cmd = new SharedBlockKvmCommands.RevertVolumeFromSnapshotCmd();
        cmd.snapshotInstallPath = makeCachedImageInstallUrlFromImageUuidForTemplate(msg.getVolume().getRootImageUuid());
        cmd.volumeUuid = msg.getVolume().getUuid();
        cmd.installPath = String.format("%s%s/%s", SharedBlockConstants.SHARED_BLOCK_INSTALL_PATH_SCHEME, msg.getPrimaryStorageUuid(), getUuid());

        new Do(msg.getVolume()).go(SharedBlockKvmCommands.REVERT_VOLUME_FROM_SNAPSHOT_PATH, cmd, RevertVolumeFromSnapshotRsp.class, new ReturnValueCompletion<RevertVolumeFromSnapshotRsp>(completion) {
            @Override
            public void success(RevertVolumeFromSnapshotRsp rsp) {
                ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();
                reply.setNewVolumeInstallPath(convertAbsolutePathToInstall(rsp.newVolumeInstallPath));
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion) {
        if (msg.hasSystemTag(VolumeSystemTags.FAST_CREATE::isMatch)) {
            createIncrementalVolumeFromSnapshot(msg, completion);
        } else {
            createNormalVolumeFromSnapshot(msg, completion);
        }
    }

    private void createIncrementalVolumeFromSnapshot(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion) {
        final String installPath = makeDataVolumeInstallUrl(msg.getVolumeUuid());
        VolumeSnapshotInventory latest = msg.getSnapshot();
        SharedBlockKvmCommands.CreateDataVolumeWithBackingCmd cmd = new SharedBlockKvmCommands.CreateDataVolumeWithBackingCmd();
        cmd.volumeUuid = latest.getVolumeUuid();
        cmd.templatePathInCache = latest.getPrimaryStorageInstallPath();
        cmd.installPath = installPath;

        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();
        if (volumeVO.getSize() != 0) {
            cmd.virtualSize = volumeVO.getSize();
        }

        new Do(VolumeInventory.valueOf(volumeVO)).go(CREATE_DATA_VOLUME_WITH_BACKING_PATH, cmd, CreateDataVolumeWithBackingRsp.class, new ReturnValueCompletion<CreateDataVolumeWithBackingRsp>(completion) {
            @Override
            public void success(CreateDataVolumeWithBackingRsp rsp) {
                CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
                reply.setActualSize(rsp.actualSize);
                reply.setInstallPath(convertAbsolutePathToInstall(installPath));
                reply.setSize(rsp.size);
                reply.setIncremental(true);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void createNormalVolumeFromSnapshot(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion) {
        final String installPath = makeDataVolumeInstallUrl(msg.getVolumeUuid());
        VolumeSnapshotInventory latest = msg.getSnapshot();
        SharedBlockKvmCommands.MergeSnapshotCmd cmd = new SharedBlockKvmCommands.MergeSnapshotCmd();
        cmd.volumeUuid = latest.getVolumeUuid();
        cmd.snapshotInstallPath = latest.getPrimaryStorageInstallPath();
        cmd.workspaceInstallPath = installPath;

        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();

        new Do(VolumeInventory.valueOf(volumeVO)).go(MERGE_SNAPSHOT_PATH, cmd, SharedBlockKvmCommands.MergeSnapshotRsp.class, new ReturnValueCompletion<SharedBlockKvmCommands.MergeSnapshotRsp>(completion) {
            @Override
            public void success(SharedBlockKvmCommands.MergeSnapshotRsp rsp) {
                CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
                reply.setActualSize(rsp.actualSize);
                reply.setInstallPath(convertAbsolutePathToInstall(installPath));
                reply.setSize(rsp.size);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void stream(VolumeSnapshotInventory from, VolumeInventory to, boolean fullRebase, final Completion completion) {
        boolean offline = true;
        VolumeInventory volume = to;

        if (volume.getType().equals(VolumeType.Memory.toString())) {
            completion.success();
            return;
        }

        boolean finalFullRebase = fullRebase || from == null;
        String finalSrcPath = from != null ? from.getPrimaryStorageInstallPath() : null;
        String finalSrcUuid = from != null ? from.getUuid() : null;

        String hostUuid = null;
        if (volume.getVmInstanceUuid() != null) {
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.state, VmInstanceVO_.hostUuid);
            q.add(VmInstanceVO_.uuid, SimpleQuery.Op.EQ, volume.getVmInstanceUuid());
            Tuple t = q.findTuple();
            VmInstanceState state = t.get(0, VmInstanceState.class);
            hostUuid = t.get(1, String.class);

            if (state != VmInstanceState.Stopped && state != VmInstanceState.Running
                    && state != VmInstanceState.Destroyed && state != VmInstanceState.Paused) {
                throw new OperationFailureException(operr("the volume[uuid;%s] is attached to a VM[uuid:%s] which is in state of %s, cannot do the snapshot merge",
                        volume.getUuid(), volume.getVmInstanceUuid(), state));
            }

            offline = (state == VmInstanceState.Stopped || state == VmInstanceState.Destroyed);
        }

        final MergeVolumeSnapshotOnPrimaryStorageReply reply = new MergeVolumeSnapshotOnPrimaryStorageReply();

        if (offline) {
            FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
            chain.setName(String.format("offline-merge-volume-snapshot-%s-on-sharedblock", finalSrcUuid));
            chain.then(new NoRollbackFlow() {
                String __name__ = String.format("deactivate-volume-%s-on-all-hosts", volume.getUuid());

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    if (!volume.isShareable()) {
                        trigger.next();
                        return;
                    }

                    ErrorCodeList errList = new ErrorCodeList();
                    List<HostInventory> hosts = sharedBlockGroupFactory.getConnectedHostsForOperation(volume.getPrimaryStorageUuid());

                    new While<>(hosts).step((hostInv, completion1) -> {
                        bulkActiveVolumes(Collections.singletonList(volume.getInstallPath()), volume.getPrimaryStorageUuid(), hostInv.getUuid(),
                                LvmlockdLockingType.NULL, true, false, new Completion(completion1) {
                            @Override
                            public void success() {
                                completion1.done();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                errList.getCauses().add(errorCode);
                                completion1.done();
                            }
                        });
                    }, 5).run(new WhileDoneCompletion(trigger) {
                        @Override
                        public void done(ErrorCodeList errorCodeList) {
                            if (!errList.getCauses().isEmpty()) {
                                trigger.fail(errList.getCauses().get(0));
                                return;
                            }
                            trigger.next();
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                String __name__ = String.format("offline-merge-volume-snapshot-%s", finalSrcUuid);

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    SharedBlockKvmCommands.OfflineMergeSnapshotCmd cmd = new SharedBlockKvmCommands.OfflineMergeSnapshotCmd();
                    cmd.fullRebase = finalFullRebase;
                    cmd.srcPath = finalSrcPath;
                    cmd.destPath = volume.getInstallPath();
                    cmd.vgUuid = volume.getPrimaryStorageUuid();
                    cmd.volumeUuid = volume.getUuid();

                    new Do(volume).go(SharedBlockKvmCommands.OFFLINE_MERGE_SNAPSHOT_PATH, cmd, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(completion) {
                        @Override
                        public void success(SharedBlockKvmCommands.AgentRsp returnValue) {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            }).done(new FlowDoneHandler(completion) {
                @Override
                public void handle(Map data) {
                    completion.success();
                }
            }).error(new FlowErrorHandler(completion) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    completion.fail(errCode);
                }
            }).start();

        } else {
            if (volume.isShareable()) {
                throw new OperationFailureException(
                        operr("not support online merge snapshot for shareable volume[uuid: %s] on sharedblock", volume.getUuid()));
            }

            FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
            chain.setName(String.format("online-merge-volume-snapshot-%s-on-sharedblock", finalSrcUuid));
            String finalHostUuid = hostUuid;
            chain.then(new NoRollbackFlow() {
                String __name__ = String.format("extend-online-merge-target-volume-%s", volume.getUuid());

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    SharedBlockKvmCommands.ExtendMergeTargetCmd cmd = new SharedBlockKvmCommands.ExtendMergeTargetCmd();
                    cmd.fullRebase = finalFullRebase;
                    cmd.srcPath = finalSrcPath;
                    cmd.destPath = volume.getInstallPath();
                    cmd.vgUuid = volume.getPrimaryStorageUuid();
                    cmd.volumeUuid = volume.getUuid();

                    new Do(finalHostUuid).go(EXTEND_MERGE_TARGET_PATH, cmd, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(completion) {
                        @Override
                        public void success(SharedBlockKvmCommands.AgentRsp returnValue) {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.next();
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                String __name__ = String.format("online-merge-volume-snapshot-%s", finalSrcUuid);

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    MergeVolumeSnapshotOnKvmMsg kmsg = new MergeVolumeSnapshotOnKvmMsg();
                    kmsg.setFullRebase(finalFullRebase);
                    kmsg.setHostUuid(finalHostUuid);
                    kmsg.setFrom(from);
                    kmsg.setTo(volume);
                    bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, finalHostUuid);
                    bus.send(kmsg, new CloudBusCallBack(completion) {
                        @Override
                        public void run(MessageReply r) {
                            if (r.isSuccess()) {
                                trigger.next();
                            } else {
                                trigger.fail(r.getError());
                            }
                        }
                    });
                }
            }).done(new FlowDoneHandler(completion) {
                @Override
                public void handle(Map data) {
                    completion.success();
                }
            }).error(new FlowErrorHandler(completion) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    completion.fail(errCode);
                }
            }).start();
        }
    }

    @Override
    void downloadImageToCache(ImageInventory img, final ReturnValueCompletion<String> completion) {
        ImageBackupStorageSelector selector = new ImageBackupStorageSelector();
        selector.setZoneUuid(self.getZoneUuid());
        selector.setImageUuid(img.getUuid());
        final String bsUuid = selector.select();
        if (bsUuid == null) {
            throw new OperationFailureException(operr(
                    "the image[uuid:%s, name: %s] is not available to download on any backup storage:\n" +
                            "1. check if image is in status of Deleted\n" +
                            "2. check if the backup storage on which the image is shown as Ready is attached to the zone[uuid:%s]",
                    img.getUuid(), img.getName(), self.getZoneUuid()
            ));
        }

        ImageBackupStorageRefInventory ref = CollectionUtils.findOneOrNull(img.getBackupStorageRefs(),
                arg -> arg.getBackupStorageUuid().equals(bsUuid));

        if (ref == null) {
            throw new OperationFailureException(operr("the image[uuid: %s, name:%s] is not found on any backup storage",
                    img.getUuid(), img.getName()));
        }

        final ImageCache cache = new ImageCache();
        cache.image = img;
        cache.primaryStorageInstallPath = makeCachedImageInstallUrl(img);
        cache.backupStorageUuid = bsUuid;
        cache.backupStorageInstallPath = ref.getInstallPath();
        cache.download(new ReturnValueCompletion<ImageCacheInventory>(completion) {
            @Override
            public void success(ImageCacheInventory returnValue) {
                completion.success(returnValue.getInstallUrl());
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void deleteBits(String path, final Completion completion) {
        deleteBits(path, false, completion);
    }

    @Override
    void deleteBits(String path, boolean folder, final Completion completion) {
        deleteBits(path, false, null, completion);
    }

    void deleteBits(String path, boolean folder, String hostUuid, final Completion completion) {
        if (path == null || path.equals("")) {
            completion.success();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-sharedblock-bits-%s", path));
        chain.then(new NoRollbackFlow() {
            String __name__ = "deactivate shared volume before delete bits";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<HostInventory> hosts;
                if (hostUuid != null) {
                    hosts = Arrays.asList(HostInventory.valueOf(dbf.findByUuid(hostUuid, HostVO.class)));
                } else {
                    hosts = sharedBlockGroupFactory.getConnectedHostsForOperation(getPrimaryStorageUuidByInstallPath(path));
                }
                if (hosts == null || hosts.isEmpty()) {
                    logger.debug(String.format("deactivate shared volume[uuid: %s] on hosts before delete bits skipped for no connected host",
                            path));
                    trigger.next();
                    return;
                }

                logger.debug(String.format("deactivate volume on hosts[uuid: %s] before delete bits since it is snapshot or sharedvolume",
                        hosts.stream().map(HostInventory::getUuid).collect(Collectors.toList())));

                new While<>(hosts).step((hostInv, completion1) -> {
                    bulkActiveVolumes(Collections.singletonList(path), getPrimaryStorageUuidByInstallPath(path), hostInv.getUuid(),
                            LvmlockdLockingType.NULL, true, false,
                            new Completion(completion1) {
                        @Override
                        public void success() {
                            completion1.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            if (errorCode.isError(VolumeErrors.VOLUME_IN_USE)) {
                                completion1.addError(errorCode);
                                completion1.allDone();
                                return;
                            }
                            completion1.done();
                        }
                    });
                }, 3).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            trigger.fail(errorCodeList.getCauses().get(0));
                            return;
                        }
                        trigger.next();
                    }
                });

            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                String __name__ = "delete volume";
                DeleteBitsCmd cmd = new DeleteBitsCmd();
                cmd.path = path;
                cmd.folder = folder;
                cmd.vgUuid = getPrimaryStorageUuidByPath(path);
                Do doDelete;

                if (hostUuid == null) {
                    doDelete = new Do(getPrimaryStorageUuidByPath(path), true);
                } else {
                    doDelete = new Do(hostUuid);
                }

                doDelete.go(DELETE_BITS_PATH, cmd, new ReturnValueCompletion<AgentRsp>(completion) {
                    @Override
                    public void success(AgentRsp rsp) {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void createImageCacheFromVolumeResource(String volumeResourceInstallPath, VolumeInventory volume, ImageInventory image, ReturnValueCompletion<ImageCacheInventory> completion) {
        ImageCache cache = new ImageCache();
        cache.volume = volume;
        cache.volumeResourceInstallPath = volumeResourceInstallPath;
        cache.image = image;
        cache.primaryStorageInstallPath = makeCachedImageInstallUrl(image);
        cache.download(new ReturnValueCompletion<ImageCacheInventory>(completion) {
            @Override
            public void success(ImageCacheInventory cache) {
                completion.success(cache);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateImageCacheFromVolumeOnPrimaryStorageReply> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("volume-sync-signature-%s", msg.getVolumeInventory().getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                final CreateImageCacheFromVolumeOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeOnPrimaryStorageReply();
                ImageCache cache = new ImageCache();
                cache.volume = msg.getVolumeInventory();
                cache.volumeResourceInstallPath = msg.getVolumeInventory().getInstallPath();
                cache.image = msg.getImageInventory();
                cache.primaryStorageInstallPath = makeCachedImageInstallUrl(msg.getImageInventory());
                cache.download(new ReturnValueCompletion<ImageCacheInventory>(completion) {
                    @Override
                    public void success(ImageCacheInventory cache) {
                        reply.setActualSize(cache.getSize());
                        completion.success(reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    @Override
    void handle(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply> completion) {
        final CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply();

        boolean useSnapshotAsCache = msg.hasSystemTag(VolumeSystemTags.FAST_CREATE.getTagFormat());
        if (useSnapshotAsCache) {
            ImageCacheVO cache = createTemporaryImageCacheFromVolumeSnapshot(msg.getImageInventory(), msg.getVolumeSnapshot());
            dbf.persist(cache);
            reply.setInventory(cache.toInventory());
            reply.setIncremental(true);
            completion.success(reply);
            return;
        }
        thdf.chainSubmit(new ChainTask(completion) {

            @Override
            public String getSyncSignature() {
                return String.format("volume-sync-signature-%s", msg.getVolumeSnapshot().getVolumeUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {

                VolumeInventory volume = VolumeInventory.valueOf(dbf.findByUuid(msg.getVolumeSnapshot().getVolumeUuid(), VolumeVO.class));
                ImageCache cache = new ImageCache();
                cache.volume = volume;
                cache.volumeResourceInstallPath = msg.getVolumeSnapshot().getPrimaryStorageInstallPath();
                cache.image = msg.getImageInventory();
                cache.primaryStorageInstallPath = makeCachedImageInstallUrl(msg.getImageInventory());
                cache.download(new ReturnValueCompletion<ImageCacheInventory>(completion) {
                    @Override
                    public void success(ImageCacheInventory inv) {
                        reply.setInventory(inv);
                        completion.success(reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void createTemplateFromVolume(final CreateTemplateFromVolumeOnPrimaryStorageMsg msg, final ReturnValueCompletion<CreateTemplateFromVolumeOnPrimaryStorageReply> completion) {
        final CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();
        final VolumeInventory volume = msg.getVolumeInventory();
        final ImageInventory image = msg.getImageInventory();

        List<HostInventory> hosts = primaryStorageFactory.getConnectedHostsForOperation(msg.getPrimaryStorageUuid(), volume);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-template-%s-from-volume-%s", image.getUuid(), volume.getUuid()));
        chain.enableProgressReport();
        chain.then(new ShareFlow() {
            final String temporaryTemplatePath = makeTemplateFromVolumeInWorkspacePath(msg.getImageInventory().getUuid());
            String backupStorageInstallPath;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-temporary-template";

                    boolean success = false;

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CreateTemplateFromVolumeCmd cmd = new CreateTemplateFromVolumeCmd();
                        cmd.volumePath = volume.getInstallPath();
                        cmd.installPath = temporaryTemplatePath;
                        cmd.sharedVolume = volume.isShareable();
                        new Do(hosts.get(0).getUuid()).go(SharedBlockKvmCommands.CREATE_TEMPLATE_FROM_VOLUME_PATH, cmd,
                                CreateTemplateFromVolumeRsp.class, new ReturnValueCompletion<CreateTemplateFromVolumeRsp>(trigger) {
                            @Override
                            public void success(CreateTemplateFromVolumeRsp rsp) {
                                reply.setActualSize(rsp.actualSize);
                                success = true;
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
                        if (success) {
                            deleteBits(temporaryTemplatePath, new Completion(trigger) {
                                @Override
                                public void success() {
                                    // pass
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    //TODO GC
                                    logger.warn(String.format("failed to delete %s, %s", temporaryTemplatePath, errorCode));
                                }
                            });
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "upload-to-backup-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        BackupStorageAskInstallPathMsg bmsg = new BackupStorageAskInstallPathMsg();
                        bmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        bmsg.setImageMediaType(image.getMediaType());
                        bmsg.setImageUuid(image.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(bmsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                        MessageReply br = bus.call(bmsg);
                        if (!br.isSuccess()) {
                            trigger.fail(br.getError());
                            return;
                        }

                        backupStorageInstallPath = ((BackupStorageAskInstallPathReply) br).getInstallPath();
                        BackupStorageSharedBlockKvmUploader uploader = getBackupStorageKvmUploader(msg.getBackupStorageUuid());
                        uploader.uploadBits(msg.getImageInventory().getUuid(), backupStorageInstallPath, temporaryTemplatePath, hosts.get(0).getUuid(), new ReturnValueCompletion<String>(trigger) {
                            @Override
                            public void success(String bsPath) {
                                backupStorageInstallPath = bsPath;
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
                    String __name__ = "delete-temporary-template-on-primary-storage";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        deleteBits(temporaryTemplatePath, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                //TODO: GC
                                logger.warn(String.format("failed to delete %s on shared mount point primary storage[uuid: %s], %s; need a cleanup", temporaryTemplatePath, self.getUuid(), errorCode));
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        reply.setFormat(volume.getFormat());
                        reply.setTemplateBackupStorageInstallPath(backupStorageInstallPath);
                        completion.success(reply);
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    void handle(final CreateTemplateFromVolumeOnPrimaryStorageMsg msg, final ReturnValueCompletion<CreateTemplateFromVolumeOnPrimaryStorageReply> completion) {
        thdf.chainSubmit(new ChainTask(completion) {

            @Override
            public String getSyncSignature() {
                return String.format("volume-sync-signature-%s", msg.getVolumeInventory().getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                createTemplateFromVolume(msg, new ReturnValueCompletion<CreateTemplateFromVolumeOnPrimaryStorageReply>(chain) {
                    @Override
                    public void success(CreateTemplateFromVolumeOnPrimaryStorageReply returnValue) {
                        completion.success(returnValue);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    void handle(UploadBitsToBackupStorageMsg msg, final ReturnValueCompletion<UploadBitsToBackupStorageReply> completion) {
        BackupStorageSharedBlockKvmUploader uploader = getBackupStorageKvmUploader(msg.getBackupStorageUuid());
        uploader.uploadBits(null, msg.getBackupStorageInstallPath(), msg.getPrimaryStorageInstallPath(), new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String bsPath) {
                UploadBitsToBackupStorageReply reply = new UploadBitsToBackupStorageReply();
                reply.setBackupStorageInstallPath(bsPath);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    void handle(CreateTemporaryVolumeFromSnapshotMsg msg, ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply> completion) {
        final String installPath = makeTemplateFromVolumeInWorkspacePath(msg.getTemporaryVolumeUuid());
        VolumeSnapshotInventory latest = msg.getSnapshot();
        SharedBlockKvmCommands.MergeSnapshotCmd cmd = new SharedBlockKvmCommands.MergeSnapshotCmd();
        cmd.volumeUuid = latest.getVolumeUuid();
        cmd.snapshotInstallPath = latest.getPrimaryStorageInstallPath();
        cmd.workspaceInstallPath = installPath;

        VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(cmd.volumeUuid, VolumeVO.class));
        new Do(vol).go(MERGE_SNAPSHOT_PATH, cmd, MergeSnapshotRsp.class, new ReturnValueCompletion<MergeSnapshotRsp>(completion) {
            @Override
            public void success(MergeSnapshotRsp mrsp) {
                CreateTemporaryVolumeFromSnapshotReply reply = new CreateTemporaryVolumeFromSnapshotReply();
                reply.setInstallPath(convertAbsolutePathToInstall(installPath));
                reply.setSize(mrsp.size);
                reply.setActualSize(mrsp.actualSize);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                deleteBits(installPath, new NopeCompletion());
                completion.fail(errorCode);
            }
        });
    }

    private List<String> findConnectedHostByClusterUuid(String clusterUuid, boolean exceptionOnNotFound) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, SimpleQuery.Op.EQ, clusterUuid);
        q.add(HostVO_.state, SimpleQuery.Op.NOT_IN, Arrays.asList(HostState.PreMaintenance, HostState.Maintenance));
        q.add(HostVO_.status, SimpleQuery.Op.EQ, HostStatus.Connected);
        List<String> hostUuids = q.listValue();
        if (hostUuids.isEmpty() && exceptionOnNotFound) {
            throw new OperationFailureException(operr("no connected host found in the cluster[uuid:%s]", clusterUuid));
        }

        return hostUuids;
    }

    @Override
    void disconnectByClusterUuid(final String clusterUuid, final Completion completion) {
        List<String> hostUuids = findConnectedHostByClusterUuid(clusterUuid, false);

        if (hostUuids.isEmpty()) {
            completion.success();
            return;
        }

        class Result {
            final Set<ErrorCode> errorCodes = new HashSet<>();
            final List<String> huuids = Collections.synchronizedList(new ArrayList<String>());
        }

        final Result ret = new Result();
        final AsyncLatch latch = new AsyncLatch(hostUuids.size(), new NoErrorCompletion(completion) {
            @Override
            public void done() {
                if (!ret.errorCodes.isEmpty()) {
                    StringBuilder errorInfo = new StringBuilder("Can't stop shared block group primary storage on ");
                    for(String hostUuid : ret.huuids) {
                        errorInfo.append(String.format("host[uuid:%s] ", hostUuid));
                    }
                    completion.fail(errf.stringToOperationError(
                            String.format("unable to stop the shared block group primary storage[uuid:%s, name:%s] from the cluster[uuid:%s], %s",
                                    self.getUuid(), self.getName(), clusterUuid, errorInfo),
                            new ArrayList<>(ret.errorCodes)
                    ));
                } else {
                    completion.success();
                }
            }
        });

        for (String huuid : hostUuids) {
            disconnect(huuid, new Completion(latch) {
                @Override
                public void success() {
                    latch.ack();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    ret.errorCodes.add(errorCode);
                    ret.huuids.add(huuid);
                    latch.ack();
                }
            });
        }
    }

    @Override
    void connectByClusterUuid(final String clusterUuid, boolean rescan, final ReturnValueCompletion<ClusterConnectionStatus> completion) {
        List<String> hosts = findConnectedHostByClusterUuid(clusterUuid, false);

        if (hosts.isEmpty()) {
            completion.success(ClusterConnectionStatus.Disconnected);
            return;
        }

        self = dbf.reload(self);
        if (self.getStatus().equals(PrimaryStorageStatus.Disconnected)) {
            changeStatus(PrimaryStorageStatus.Connecting);
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("connect-shared-block-primary-storage-%s-on-cluster-%s", self.getUuid(), clusterUuid));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                doCheckByClusterUuid(clusterUuid, hosts, rescan, new Completion(trigger) {
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
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                doConnectByClusterUuid(clusterUuid, hosts, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode); }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(ClusterConnectionStatus.FullyConnected);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                if (self.getStatus().equals(PrimaryStorageStatus.Connecting)) {
                    changeStatus(PrimaryStorageStatus.Disconnected);
                }
                completion.fail(errCode);
            }
        }).start();

    }

    void doConnectByClusterUuid(final String clusterUuid, List<String> hostUuids, final Completion completion) {
        if (hostUuids == null || hostUuids.isEmpty()) {
            completion.success();
            return;
        }

        class Result {
            final Set<ErrorCode> errorCodes = new HashSet<>();
            final List<String> huuids = Collections.synchronizedList(new ArrayList<String>());
            final List<SharedBlockKvmCommands.ConnectRsp> firstAccessHosts = Collections.synchronizedList(new ArrayList<SharedBlockKvmCommands.ConnectRsp>());
        }

        final Result ret = new Result();
        final AsyncLatch latch = new AsyncLatch(hostUuids.size(), new NoErrorCompletion(completion) {
            @Override
            public void done() {
                if (ret.firstAccessHosts.size() > 1 && ret.firstAccessHosts.stream().map(h -> h.vgLvmUuid).collect(Collectors.toSet()).size() > 1) {
                    ret.errorCodes.add(operr(
                            "hosts[uuid:%s] have the disk uuid of shared block, but actually different storage.",
                            ret.firstAccessHosts.stream().map(h -> h.hostUuid).collect(Collectors.toList())
                    ));
                }

                if (!ret.errorCodes.isEmpty()) {
                    StringBuilder errorInfo = new StringBuilder("Can't access shared block group primary storage on ");
                    for(String hostUuid : ret.huuids) {
                        errorInfo.append(String.format("host[uuid:%s] ", hostUuid));
                    }
                    completion.fail(errf.stringToOperationError(
                            String.format("unable to connect the shared block group primary storage[uuid:%s, name:%s] to the cluster[uuid:%s], %s, details: %s",
                                    self.getUuid(), self.getName(), clusterUuid, errorInfo, ret.errorCodes.iterator().next().getDetails()),
                            new ArrayList<>(ret.errorCodes)
                    ));
                } else {
                    completion.success();
                }
            }
        });

        for (String huuid : hostUuids) {
            changeSharedBlockGroupPrimaryStorageHostRefStatus(self.getUuid(), huuid, PrimaryStorageHostStatus.Connecting);
            connect(huuid, new ReturnValueCompletion<SharedBlockKvmCommands.ConnectRsp>(latch) {
                @Override
                public void success(SharedBlockKvmCommands.ConnectRsp connectRsp) {
                    if(connectRsp.isFirst){
                        connectRsp.hostUuid = huuid;
                        ret.firstAccessHosts.add(connectRsp);
                    }
                    latch.ack();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    ret.errorCodes.add(errorCode);
                    ret.huuids.add(huuid);
                    latch.ack();
                }
            });
        }
    }

    void doCheckByClusterUuid(final String clusterUuid, List<String> hostUuids, boolean rescan, final Completion completion) {
        List<String> sharedBlockDiskUuids = Q.New(SharedBlockVO.class)
                .select(SharedBlockVO_.diskUuid)
                .eq(SharedBlockVO_.sharedBlockGroupUuid, self.getUuid())
                .listValues();

        if (hostUuids == null || hostUuids.isEmpty()) {
            completion.success();
            return;
        }

        class Result {
            final Set<ErrorCode> errorCodes = new HashSet<>();
            final List<String> huuids = Collections.synchronizedList(new ArrayList<String>());
        }

        final Result ret = new Result();
        final AsyncLatch latch = new AsyncLatch(hostUuids.size(), new NoErrorCompletion(completion) {
            @Override
            public void done() {
                if (!ret.errorCodes.isEmpty()) {
                    StringBuilder errorInfo = new StringBuilder(String.format("Can't access shared block[diskUuid: %s] on ", sharedBlockDiskUuids));
                    for(String hostUuid : ret.huuids) {
                        errorInfo.append(String.format("host[uuid:%s] ", hostUuid));
                    }
                    completion.fail(errf.stringToOperationError(
                            String.format("unable to connect the shared block group primary storage[uuid:%s, name:%s] to the cluster[uuid:%s], %s",
                                    self.getUuid(), self.getName(), clusterUuid, errorInfo),
                            new ArrayList<>(ret.errorCodes)
                    ));
                } else {
                    List<SharedBlockVO> sharedBlockVOS = Q.New(SharedBlockVO.class)
                            .eq(SharedBlockVO_.sharedBlockGroupUuid, self.getUuid()).list();
                    for (SharedBlockVO vo : sharedBlockVOS) {
                        vo.setStatus(SharedBlockStatus.Connected);
                    }
                    dbf.updateCollection(sharedBlockVOS);
                    completion.success();
                }
            }
        });

        for (String huuid : hostUuids) {
            checkDisk(huuid, self.getUuid(), sharedBlockDiskUuids, rescan, new ReturnValueCompletion<Boolean>(latch) {
                @Override
                public void success(Boolean returnValue) {
                    latch.ack();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    ret.errorCodes.add(errorCode);
                    ret.huuids.add(huuid);
                    latch.ack();
                }
            });
        }
    }

    void doCheckByClusterUuid(final String clusterUuid, String sharedBlockGroupUuid, String diskUuid, final Completion completion) {
        List<HostInventory> hostUuids = primaryStorageFactory.getConnectedHostsForOperation(sharedBlockGroupUuid)
                .stream()
                .filter(h -> h.getClusterUuid().equals(clusterUuid))
                .collect(Collectors.toList());

        if (hostUuids.isEmpty()) {
            completion.success();
            return;
        }

        class Result {
            final Set<ErrorCode> errorCodes = new HashSet<>();
            final List<String> huuids = Collections.synchronizedList(new ArrayList<String>());
        }

        final Result ret = new Result();
        final AsyncLatch latch = new AsyncLatch(hostUuids.size(), new NoErrorCompletion(completion) {
            @Override
            public void done() {
                if (!ret.errorCodes.isEmpty()) {
                    StringBuilder errorInfo = new StringBuilder(String.format("can't access shared block[diskUuid: %s] on ", diskUuid));
                    for(String hostUuid : ret.huuids) {
                        errorInfo.append(String.format("host[uuid:%s] ", hostUuid));
                    }
                    completion.fail(errf.stringToOperationError(
                            String.format("unable to add the shared block to group[uuid:%s, name:%s] to the cluster[uuid:%s], %s",
                                    self.getUuid(), self.getName(), clusterUuid, errorInfo),
                            new ArrayList<>(ret.errorCodes)
                    ));
                } else {
                    List<SharedBlockVO> sharedBlockVOS = Q.New(SharedBlockVO.class)
                            .eq(SharedBlockVO_.sharedBlockGroupUuid, self.getUuid()).list();
                    for (SharedBlockVO vo : sharedBlockVOS) {
                        vo.setStatus(SharedBlockStatus.Connected);
                    }
                    dbf.updateCollection(sharedBlockVOS);
                    completion.success();
                }
            }
        });

        for (HostInventory host : hostUuids) {
            checkDisk(host.getUuid(), self.getUuid(), Collections.singletonList(diskUuid), false, new ReturnValueCompletion<Boolean>(latch) {
                @Override
                public void success(Boolean returnValue) {
                    latch.ack();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    ret.errorCodes.add(errorCode);
                    ret.huuids.add(host.getUuid());
                    latch.ack();
                }
            });
        }
    }

    private void checkDisk(String hostUuid, String primaryStorageUuid, List<String> sharedBlockDiskUuids, boolean rescan, final ReturnValueCompletion<Boolean> completion) {
        SharedBlockKvmCommands.CheckDisksCmd cmd = new SharedBlockKvmCommands.CheckDisksCmd();
        cmd.sharedBlockUuids = sharedBlockDiskUuids;
        cmd.hostUuid = hostUuid;
        cmd.vgUuid = primaryStorageUuid;
        cmd.rescan_scsi = rescan;

        httpCall(SharedBlockKvmCommands.CHECK_DISKS_PATH, hostUuid, cmd, true, SharedBlockKvmCommands.AgentRsp.class, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(completion) {
            @Override
            public void success(SharedBlockKvmCommands.AgentRsp rsp) {
                completion.success(true);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                changeSharedBlockGroupPrimaryStorageHostRefStatus(primaryStorageUuid, hostUuid, PrimaryStorageHostStatus.Disconnected);
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(SyncVolumeSizeOnPrimaryStorageMsg msg, final ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply> completion) {
        // NOTE(weiw): not support sync volume size since it is useless
        VolumeVO vo = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();
        if (vo == null) {
            completion.fail(operr("can not find volume[uuid: %s]", msg.getVolumeUuid()));
            return;
        }

        SharedBlockKvmCommands.GetVolumeSizeCmd cmd = new SharedBlockKvmCommands.GetVolumeSizeCmd();
        cmd.vgUuid = msg.getPrimaryStorageUuid();
        cmd.installPath = msg.getInstallPath();
        cmd.volumeUuid = msg.getVolumeUuid();

        new Do(VolumeInventory.valueOf(vo)).go(GET_VOLUME_SIZE_PATH, cmd, GetVolumeSizeRsp.class, new ReturnValueCompletion<GetVolumeSizeRsp>(completion) {
            @Override
            public void success(GetVolumeSizeRsp rsp) {
                SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
                reply.setActualSize(rsp.actualSize);
                reply.setSize(rsp.size);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(EstimateVolumeTemplateSizeOnPrimaryStorageMsg msg, ReturnValueCompletion<EstimateVolumeTemplateSizeOnPrimaryStorageReply> completion) {
        VolumeVO vo = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();
        if (vo == null) {
            completion.fail(operr("can not find volume[uuid: %s]", msg.getVolumeUuid()));
            return;
        }

        SharedBlockKvmCommands.EstimateTemplateSizeCmd cmd = new SharedBlockKvmCommands.EstimateTemplateSizeCmd();
        cmd.vgUuid = msg.getPrimaryStorageUuid();
        cmd.volumePath = msg.getInstallPath();

        new Do(VolumeInventory.valueOf(vo)).go(ESTIMATE_TEMPLATE_SIZE_PATH, cmd, EstimateTemplateSizeRsp.class, new ReturnValueCompletion<EstimateTemplateSizeRsp>(completion) {
            @Override
            public void success(EstimateTemplateSizeRsp rsp) {
                EstimateVolumeTemplateSizeOnPrimaryStorageReply reply = new EstimateVolumeTemplateSizeOnPrimaryStorageReply();
                reply.setActualSize(rsp.actualSize);
                reply.setSize(rsp.size);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(BatchSyncVolumeSizeOnPrimaryStorageMsg msg, final ReturnValueCompletion<BatchSyncVolumeSizeOnPrimaryStorageReply> completion) {
        SharedBlockKvmCommands.GetBatchVolumeSizeCmd cmd = new SharedBlockKvmCommands.GetBatchVolumeSizeCmd();
        cmd.vgUuid = msg.getPrimaryStorageUuid();
        cmd.volumeUuidInstallPaths = msg.getVolumeUuidInstallPaths();

        new Do(msg.getHostUuid()).go(BATCH_GET_VOLUME_SIZE_PATH, cmd, GetBatchVolumeSizeRsp.class, new ReturnValueCompletion<GetBatchVolumeSizeRsp>(completion) {
            @Override
            public void success(GetBatchVolumeSizeRsp rsp) {
                BatchSyncVolumeSizeOnPrimaryStorageReply reply = new BatchSyncVolumeSizeOnPrimaryStorageReply();
                reply.setActualSizes(rsp.actualSizes);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("Get sblk primary storage [uuid: %s] volume size fail, the reason is as followed : %s", cmd.vgUuid, errorCode.getDescription()));
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg, final ReturnValueCompletion<BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply> completion) {
        VolumeSnapshotInventory sinv = msg.getSnapshot();
        String bsUuid = msg.getBackupStorage().getUuid();

        // Get the backup storage install path
        BackupStorageAskInstallPathMsg bmsg = new BackupStorageAskInstallPathMsg();
        bmsg.setImageMediaType(VolumeSnapshotVO.class.getSimpleName());
        bmsg.setBackupStorageUuid(msg.getBackupStorage().getUuid());
        bmsg.setImageUuid(sinv.getUuid());
        bus.makeTargetServiceIdByResourceUuid(bmsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorage().getUuid());
        MessageReply br = bus.call(bmsg);
        if (!br.isSuccess()) {
            completion.fail(br.getError());
            return;
        }

        String hostUuid = primaryStorageFactory.getConnectedHostsForOperation(getSelfInventory()).get(0).getUuid();
        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getSnapshot().getVolumeUuid()).find();
        if (volumeVO.isAttached() && volumeVO.getVmInstanceUuid() != null) {
            VmInstanceVO instanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, volumeVO.getVmInstanceUuid()).find();
            if (instanceVO.getHostUuid() != null) {
                hostUuid = instanceVO.getHostUuid();
            }
        }

        final String installPath = ((BackupStorageAskInstallPathReply) br).getInstallPath();
        BackupStorageSharedBlockKvmUploader uploader = getBackupStorageKvmUploader(bsUuid);
        uploader.uploadBits(null, installPath, sinv.getPrimaryStorageInstallPath(), hostUuid, new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String bsPath) {
                BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply reply = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply();
                reply.setBackupStorageInstallPath(bsPath);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    void getPhysicalCapacity(PrimaryStorageInventory inv, ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        SharedBlockKvmCommands.CheckBitsCmd cmd = new SharedBlockKvmCommands.CheckBitsCmd();
        cmd.path = makeInstallPath(null);
        new Do().go(SharedBlockKvmCommands.CHECK_BITS_PATH, cmd, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(completion) {
            @Override
            public void success(SharedBlockKvmCommands.AgentRsp rsp) {
                PhysicalCapacityUsage usage = new PhysicalCapacityUsage();
                usage.totalPhysicalSize = rsp.totalCapacity;
                usage.availablePhysicalSize = rsp.availableCapacity;
                completion.success(usage);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handleHypervisorSpecificMessage(SharedBlockGroupPrimaryStorageHypervisorSpecificMessage msg) {
        if (msg instanceof InitKvmHostMsg) {
            handle((InitKvmHostMsg) msg);
        } else if (msg instanceof DisconnectSharedBlockGroupPrimaryStorageOnHostMsg) {
            handle((DisconnectSharedBlockGroupPrimaryStorageOnHostMsg) msg);
        } else if (msg instanceof SharedBlockConvertVolumeProvisioningMsg) {
            handle((SharedBlockConvertVolumeProvisioningMsg) msg);
        } else if (msg instanceof PingHostAttachedPrimaryStoragesMsg) {
            handle((PingHostAttachedPrimaryStoragesMsg) msg);
        } else {
            bus.dealWithUnknownMessage((Message) msg);
        }
    }

    private void filterExclusiveLockOpForCheckLock(List<String> primaryStorageUuids) {
        Set<String> tmp = new HashSet<>(primaryStorageUuids);
        primaryStorageUuids.clear();
        for (String ps : tmp) {
            if (!exclusiveLockOpInPrimaryStorage.containsKey(ps) || exclusiveLockOpInPrimaryStorage.get(ps).size() == 0) {
                primaryStorageUuids.add(ps);
            }
        }
    }

    private void handle(final PingHostAttachedPrimaryStoragesMsg msg) {
        thdf.singleFlightSubmit(new SingleFlightTask(msg)
                .setSyncSignature(String.format("ping-attached-sharedblock-primary-storages-on-host-%s", msg.getHostUuid()))
                .run(completion -> {
                    CheckLockCmd cmd = new CheckLockCmd();
                    cmd.vgUuids = msg.getPrimaryStorageUuids();
                    new Do(msg.getHostUuid()).go(CHECK_STATE_PATH, cmd, CheckLockRsp.class, new ReturnValueCompletion<CheckLockRsp>(completion) {
                        @Override
                        public void success(CheckLockRsp checkLockRsp) {
                            completion.success(checkLockRsp.failedVgs);
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.debug(String.format("check sharedblock[%s] lock on host %s failed: %s", msg.getPrimaryStorageUuids(),
                                    msg.getHostUuid(), errorCode.getDescription()));
                            completion.fail(errorCode);
                        }
                    });
                }).done(result -> {
                    if (!result.isSuccess() || result.getResult() == null) {
                        return;
                    }
                    Map<String, String> checkLockResult = ((Map<String, String>)result.getResult());
                    if (checkLockResult.isEmpty()) {
                        return;
                    }
                    List<String> connectedPsUuids = Q.New(SharedBlockGroupPrimaryStorageHostRefVO.class)
                            .eq(SharedBlockGroupPrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Connected)
                            .eq(SharedBlockGroupPrimaryStorageHostRefVO_.hostUuid, msg.getHostUuid())
                            .in(SharedBlockGroupPrimaryStorageHostRefVO_.primaryStorageUuid, checkLockResult.keySet())
                            .select(SharedBlockGroupPrimaryStorageHostRefVO_.primaryStorageUuid)
                            .listValues();
                    for (Map.Entry<String, String> entry : ((Map<String, String>)result.getResult()).entrySet()) {
                        if (!connectedPsUuids.contains(entry.getKey())) {
                            continue;
                        }
                        UpdatePrimaryStorageHostStatusMsg umsg = new UpdatePrimaryStorageHostStatusMsg();
                        umsg.setStatus(PrimaryStorageHostStatus.Disconnected);
                        umsg.setPrimaryStorageUuid(entry.getKey());

                        ErrorCode reason = HaConstants.PRIMARY_STORAGE_HOST_DISCONNECTED_ERROR;
                        reason.setDetails(String.format("primary storage %s disconnect on host %s, details: %s",
                                entry.getKey(), msg.getHostUuid(), entry.getValue()));
                        umsg.setReason(reason);
                        umsg.setHostUuid(msg.getHostUuid());
                        bus.makeTargetServiceIdByResourceUuid(umsg, PrimaryStorageConstant.SERVICE_ID, entry.getKey());
                        bus.send(umsg);
                    }
                }));
    }
    private void handle(final DisconnectSharedBlockGroupPrimaryStorageOnHostMsg msg) {
        final DisconnectSharedBlockGroupPrimaryStorageOnHostReply reply = new DisconnectSharedBlockGroupPrimaryStorageOnHostReply();
        disconnect(msg.getHostUuid(), msg.getPrimaryStorageUuid(), msg.getStopService(), new Completion(msg) {
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

    private void handle(final SharedBlockConvertVolumeProvisioningMsg msg) {
        SharedBlockConvertVolumeProvisioningReply reply = new SharedBlockConvertVolumeProvisioningReply();
        if (msg.getProvisioningStrategy().equals(VolumeProvisioningStrategy.ThickProvisioning)) {
            reply.setError(Platform.operr("not support convert thin volume to thick volume yet"));
            bus.reply(msg, reply);
            return;
        }

        ConvertVolumeProvisioningCmd cmd = new ConvertVolumeProvisioningCmd();
        cmd.setInstallPath(msg.getInstallPath());
        cmd.setVolumeUuid(msg.getVolumeUuid());
        cmd.setProvisioningStrategy(msg.getProvisioningStrategy().toString());
        cmd.vgUuid = msg.getPrimaryStorageUuid();

        new Do(msg.getHostUuid(), msg.getPrimaryStorageUuid()).go(CONVERT_VOLUME_PROVISIONING_PATH, cmd, ConvertVolumeProvisioningRsp.class, new ReturnValueCompletion<ConvertVolumeProvisioningRsp>(msg) {
            @Override
            public void success(ConvertVolumeProvisioningRsp r) {
                VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();
                if (volumeVO == null || !volumeVO.getInstallPath().equals(msg.getInstallPath())) {
                    //NOTE(weiw): not update size if convert snapshot
                    bus.reply(msg, reply);
                    return;
                }

                // the actual size = volume actual size + all snapshot size
                long snapshotSize = calculateSnapshotSize();
                volumeVO.setActualSize(r.actualSize + snapshotSize);
                dbf.updateAndRefresh(volumeVO);

                bus.reply(msg, reply);
            }

            @Transactional(readOnly = true)
            private long calculateSnapshotSize() {
                String sql = "select sum(sp.size) from VolumeSnapshotVO sp where sp.volumeUuid = :uuid";
                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("uuid", msg.getVolumeUuid());
                Long size = q.getSingleResult();
                return size == null ? 0 : size;
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final InitKvmHostMsg msg) {
        final InitKvmHostReply reply = new InitKvmHostReply();
        SharedBlockGroupPrimaryStorageHostRefVO vo = Q.New(SharedBlockGroupPrimaryStorageHostRefVO.class)
                .eq(SharedBlockGroupPrimaryStorageHostRefVO_.hostUuid, msg.getHostUuid())
                .eq(SharedBlockGroupPrimaryStorageHostRefVO_.primaryStorageUuid, msg.getPrimaryStorageUuid())
                .find();
        if (msg.getExpectStatus() != null && vo != null && !msg.getExpectStatus().equals(vo.getStatus())) {
            reply.setError(operr("expected status is %s and current status"));
            bus.reply(msg, reply);
            return;
        }

        changeSharedBlockGroupPrimaryStorageHostRefStatus(msg.getPrimaryStorageUuid(), msg.getHostUuid(), PrimaryStorageHostStatus.Connecting);
        List<String> sharedBlockDiskUuids = Q.New(SharedBlockVO.class)
                .select(SharedBlockVO_.diskUuid)
                .eq(SharedBlockVO_.sharedBlockGroupUuid, msg.getPrimaryStorageUuid())
                .listValues();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("init-sharedblock-%s-on-kvm-host-%s", msg.getPrimaryStorageUuid(), msg.getHostUuid()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                checkDisk(msg.getHostUuid(), msg.getPrimaryStorageUuid(), sharedBlockDiskUuids, false, new ReturnValueCompletion<Boolean>(trigger) {
                    @Override
                    public void success(Boolean returnValue) {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                connect(msg.getHostUuid(), msg.getPrimaryStorageUuid(), new ReturnValueCompletion<SharedBlockKvmCommands.ConnectRsp>(msg) {
                    @Override
                    public void success(SharedBlockKvmCommands.ConnectRsp connectRsp) {
                        if(connectRsp.isFirst){
                            logger.warn(String.format("host[uuid:%s] might mount storage which is different from shared block " +
                                            "group primary storage [uuid:%s], please check it",
                                    msg.getHostUuid(), msg.getPrimaryStorageUuid()));
                        }
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        trigger.fail(errorCode);
                    }
                });
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                changeSharedBlockGroupPrimaryStorageHostRefStatus(msg.getPrimaryStorageUuid(), msg.getHostUuid(), PrimaryStorageHostStatus.Disconnected);
                bus.reply(msg, reply);
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, reply);
            }
        }).start();
    }

    @Override
    public boolean start() {
        evtf.on(SharedBlockPrimaryStorageCanonicalEvent.UPDATE_ACTIVATE_VOLUME_GC_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                SharedBlockPrimaryStorageCanonicalEvent.UpdateActivateVolumeGC gcData = (SharedBlockPrimaryStorageCanonicalEvent.UpdateActivateVolumeGC) data;
                if ("remove".equals(gcData.getAction())) {
                    activateVolumeGCs.remove(gcData.getGcUuid());
                } else if ("add".equals(gcData.getAction())) {
                    activateVolumeGCs.put(gcData.getGcUuid(), gcData.getContext());
                }
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void afterVolumeLiveSnapshotGroupCreatedOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmReply treply, Completion completion) {
        ErrorCodeList errorCodes = new ErrorCodeList();
        if (treply == null || treply.getSnapshotsResults() == null || treply.getSnapshotsResults().isEmpty()) {
            completion.success();
            return;
        }

        new While<>(treply.getSnapshotsResults()).step((s, whileCompletion) -> {
            String psUuid = getPrimaryStorageUuidByAbsolutePath(s.getInstallPath());
            if (!isSharedBlockGroupPrimaryStorage(psUuid)) {
                whileCompletion.done();
                return;
            }

            VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, s.getVolumeUuid()).find();
            volumeVO.setInstallPath(convertAbsolutePathToInstall(volumeVO.getInstallPath()));
            dbf.update(volumeVO);

            // skip non disk snapshot
            if (!volumeVO.isDisk()) {
                whileCompletion.done();
                return;
            }

            VolumeSnapshotVO snapshotVO = Q.New(VolumeSnapshotVO.class)
                    .eq(VolumeSnapshotVO_.primaryStorageInstallPath, s.getPreviousInstallPath()).find();
            snapshotVO.setPrimaryStorageInstallPath(convertAbsolutePathToInstall(snapshotVO.getPrimaryStorageInstallPath()));
            dbf.update(snapshotVO);

            final String doHostUuid = primaryStorageFactory
                    .getConnectedHostsForOperation(volumeVO.getPrimaryStorageUuid(), VolumeInventory.valueOf(volumeVO))
                    .stream()
                    .map(HostInventory::getUuid)
                    .findFirst().orElse(null);

            FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
            chain.setName(String.format("after-take-live-snapshots-%s-for-volume-%s", snapshotVO.getUuid(), volumeVO.getUuid()));
            chain.then(new NoRollbackFlow() {
                String __name__= String.format("shrink-snapShot-%s", snapshotVO.getUuid());

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    if (!SharedBlockGlobalConfig.SHRINK_SNAPSHOT.value(Boolean.class) ||
                            snapshotVO.isFullSnapshot()) {
                        trigger.next();
                        return;
                    }

                    ShrinkSnapshotCmd scmd = new ShrinkSnapshotCmd();
                    scmd.installPath = snapshotVO.getPrimaryStorageInstallPath();
                    scmd.vgUuid = snapshotVO.getPrimaryStorageUuid();
                    new Do(doHostUuid).go(SHRINK_SNAPSHOT_PATH, scmd, ShrinkSnapshotRsp.class, new ReturnValueCompletion<ShrinkSnapshotRsp>(trigger) {
                        @Override
                        public void success(ShrinkSnapshotRsp reply) {
                            logger.debug(String.format("successfully shrink sblk snapshot[%s] from %s to %s ", snapshotVO.getPrimaryStorageInstallPath(), reply.oldSize, reply.size));
                            long deltaSize = reply.oldSize - reply.size;
                            if (deltaSize != 0) {
                                snapshotVO.setSize(reply.size);
                                dbf.update(snapshotVO);

                                logger.debug(String.format("after shrink sblk snapshot, release snapshot size[%s] on primary storage[%s] for volume[uuid:%s]",
                                        deltaSize, volumeVO.getPrimaryStorageUuid(), volumeVO.getUuid()));
                                ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                                rmsg.setPrimaryStorageUuid(volumeVO.getPrimaryStorageUuid());
                                rmsg.setDiskSize(deltaSize);
                                rmsg.setNoOverProvisioning(true);
                                bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, volumeVO.getPrimaryStorageUuid());
                                bus.send(rmsg);
                            }

                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format("shrink sblk snapshot[%s] failed:%n%s",
                                    snapshotVO.getPrimaryStorageInstallPath(), errorCode.getReadableDetails()));
                            trigger.next();
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                String __name__= String.format("active-snapShot-%s", snapshotVO.getUuid());

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    activeVolumeByInstallPath(s.getPreviousInstallPath(), psUuid, doHostUuid, LvmlockdLockingType.SHARE, false, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            errorCodes.getCauses().add(errorCode);
                            trigger.next();
                        }
                    });
                }
            }).error(new FlowErrorHandler(whileCompletion) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    whileCompletion.done();
                }
            }).done(new FlowDoneHandler(whileCompletion) {
                @Override
                public void handle(Map data) {
                    whileCompletion.done();
                }
            }).start();

        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodes.getCauses().isEmpty()) {
                    completion.fail(errorCodes.getCauses().get(0));
                    return;
                }

                completion.success();
            }
        });
    }

    @Override
    public void afterVolumeLiveSnapshotGroupCreationFailsOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmReply treply) {
        class VmVolumesStruct {
            public VmInstanceVO vmInstanceVO;
            public List<CreateVolumesSnapshotsJobStruct> jobs;
        }

        List<CreateVolumesSnapshotsJobStruct> sharedBlockVolumeSnapshotJobs = new ArrayList<>();

        for (CreateVolumesSnapshotsJobStruct struct : msg.getVolumeSnapshotJobs()) {
            if (isSharedBlockGroupPrimaryStorage(struct.getPrimaryStorageUuid())) {
                sharedBlockVolumeSnapshotJobs.add(struct);
            }
        }

        if (sharedBlockVolumeSnapshotJobs.isEmpty()) {
            return;
        }

        if (msg.getLockedVmInstanceUuids() == null || msg.getLockedVmInstanceUuids().isEmpty()) {
            return;
        }

        List<VmVolumesStruct> vmVolumesStructs = Collections.synchronizedList(new ArrayList<>());
        for (String vmUuid : msg.getLockedVmInstanceUuids()) {
            VmVolumesStruct vmVolumesStruct = new VmVolumesStruct();
            vmVolumesStruct.vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).find();
            List<String> vmVolumesUuids = vmVolumesStruct.vmInstanceVO.getAllDiskVolumes()
                    .stream().map(VolumeVO::getUuid).collect(Collectors.toList());
            vmVolumesStruct.jobs = msg.getVolumeSnapshotJobs().stream()
                    .filter(s -> vmVolumesUuids.contains(s.getVolumeUuid())
                            || (vmVolumesStruct.vmInstanceVO.getMemoryVolume() != null
                            && s.getVolumeUuid().equals(vmVolumesStruct.vmInstanceVO.getMemoryVolume().getUuid())))
                    .collect(Collectors.toList());
            vmVolumesStructs.add(vmVolumesStruct);
        }

        for (VmVolumesStruct vmVolumesStruct : vmVolumesStructs) {
            if (vmVolumesStruct.jobs == null || vmVolumesStruct.jobs.isEmpty()) {
                return;
            }

            if (vmVolumesStruct.vmInstanceVO.getState().equals(VmInstanceState.Stopped)) {
                return;
            }

            for (CreateVolumesSnapshotsJobStruct job : vmVolumesStruct.jobs) {
                String path = makeInstallPath(job.getPrimaryStorageUuid(), job.getVolumeSnapshotStruct().getCurrent().getUuid());
                deleteBits(path, new NopeCompletion());
            }
        }
    }

    @Override
    public void afterVolumeSnapshotGroupCreated(VolumeSnapshotGroupInventory snapshotGroup, ConsistentType consistentType, Completion completion) {
        completion.success();
    }

    @Override
    public void afterVolumeSnapshotCreated(VolumeSnapshotInventory snapshot, Completion completion) {
        completion.success();
    }

    class ImageCache {
        ImageInventory image;
        String backupStorageUuid;
        String primaryStorageInstallPath;
        String backupStorageInstallPath;
        VolumeInventory volume;
        String volumeResourceInstallPath;

        void download(final ReturnValueCompletion<ImageCacheInventory> completion) {
            DebugUtils.Assert(image != null, "image cannot be null");
            DebugUtils.Assert(primaryStorageInstallPath != null, "primaryStorageInstallPath cannot be null");

            thdf.chainSubmit(new ChainTask(completion) {
                @Override
                public String getSyncSignature() {
                    return String.format("download-image-%s-to-shared-block-storage-%s-cache", image.getUuid(), self.getUuid());
                }

                private void doDownload(final SyncTaskChain chain) {
                    if (volume == null) {
                        DebugUtils.Assert(backupStorageUuid != null, "backup storage UUID cannot be null");
                        DebugUtils.Assert(backupStorageInstallPath != null, "backupStorageInstallPath cannot be null");
                    }

                    FlowChain fchain = FlowChainBuilder.newShareFlowChain();
                    fchain.setName(String.format("download-image-%s-to-shared-block-storage-%s-cache",
                            image.getUuid(), self.getUuid()));
                    fchain.then(new ShareFlow() {
                        long actualSize = image.getActualSize();
                        ImageCacheVO vo = new ImageCacheVO();

                        @Override
                        public void setup() {
                            flow(new Flow() {
                                String __name__ = "allocate-primary-storage";

                                boolean s = false;

                                @Override
                                public void run(final FlowTrigger trigger, Map data) {
                                    AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
                                    amsg.setRequiredPrimaryStorageUuid(self.getUuid());
                                    amsg.setSize(image.getActualSize());
                                    amsg.setPurpose(PrimaryStorageAllocationPurpose.DownloadImage.toString());
                                    amsg.setNoOverProvisioning(true);
                                    bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                                    bus.send(amsg, new CloudBusCallBack(trigger) {
                                        @Override
                                        public void run(MessageReply reply) {
                                            if (reply.isSuccess()) {
                                                s = true;
                                                trigger.next();
                                            } else {
                                                trigger.fail(reply.getError());
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void rollback(FlowRollback trigger, Map data) {
                                    if (s) {
                                        IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                                        imsg.setDiskSize(image.getActualSize());
                                        imsg.setNoOverProvisioning(true);
                                        imsg.setPrimaryStorageUuid(self.getUuid());
                                        bus.makeLocalServiceId(imsg, PrimaryStorageConstant.SERVICE_ID);
                                        bus.send(imsg);
                                    }

                                    trigger.rollback();
                                }
                            });

                            flow(new NoRollbackFlow() {
                                String __name__ = "download";

                                @Override
                                public void run(final FlowTrigger trigger, Map data) {
                                    if (volume != null) {
                                        downloadFromVolume(trigger);
                                    } else {
                                        downloadFromBackupStorage(trigger);
                                    }
                                }

                                private void downloadFromVolume(FlowTrigger trigger) {
                                    CreateImageCacheFromVolumeCmd cmd = new CreateImageCacheFromVolumeCmd();
                                    cmd.volumePath = volumeResourceInstallPath;
                                    cmd.installPath = primaryStorageInstallPath;
                                    new Do(volume).go(SharedBlockKvmCommands.CREATE_IMAGE_CACHE_FROM_VOLUME_PATH, cmd, CreateImageCacheFromVolumeRsp.class, new ReturnValueCompletion<CreateImageCacheFromVolumeRsp>(trigger) {
                                        @Override
                                        public void success(CreateImageCacheFromVolumeRsp rsp) {
                                            actualSize = rsp.actualSize;
                                            trigger.next();
                                        }

                                        @Override
                                        public void fail(ErrorCode errorCode) {
                                            trigger.fail(errorCode);
                                        }
                                    });
                                }

                                private void downloadFromBackupStorage(FlowTrigger trigger) {
                                    BackupStorageSharedBlockKvmDownloader downloader = getBackupStorageKvmDownloader(backupStorageUuid);
                                    downloader.downloadBits(backupStorageInstallPath, primaryStorageInstallPath, LvmlockdLockingType.SHARE, new Completion(trigger) {
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
                                String __name__ = "save-db";
                                @Override
                                public void run(FlowTrigger trigger, Map data) {
                                    vo.setState(ImageCacheState.ready);
                                    vo.setMediaType(ImageConstant.ImageMediaType.valueOf(image.getMediaType()));
                                    vo.setImageUuid(image.getUuid());
                                    vo.setPrimaryStorageUuid(self.getUuid());
                                    vo.setSize(actualSize);
                                    vo.setMd5sum("not calculated");
                                    vo.setInstallUrl(primaryStorageInstallPath);
                                    dbf.persist(vo);
                                    logger.debug(String.format("downloaded image[uuid:%s, name:%s] to the image cache of local shared block storage[uuid: %s, installPath: %s]",
                                            image.getUuid(), image.getName(), self.getUuid(), primaryStorageInstallPath));
                                    trigger.next();
                                }
                            });

                            flow(new NoRollbackFlow() {
                                String __name__ = "invoke-after-create-image-cache-extensions";

                                @Override
                                public void run(FlowTrigger trigger, Map data) {
                                    ImageCacheInventory inventory = ImageCacheInventory.valueOf(vo);

                                    new While<>(pluginRgty.getExtensionList(AfterCreateImageCacheExtensionPoint.class)).each((ext, whileCompletion) -> {
                                        ext.saveEncryptAfterCreateImageCache(null, inventory, new Completion(whileCompletion) {
                                            @Override
                                            public void success() {
                                                whileCompletion.done();
                                            }

                                            @Override
                                            public void fail(ErrorCode errorCode) {
                                                whileCompletion.addError(errorCode);
                                                whileCompletion.done();
                                            }
                                        });
                                    }).run(new WhileDoneCompletion(trigger) {
                                        @Override
                                        public void done(ErrorCodeList errorCodeList) {
                                            if (!errorCodeList.getCauses().isEmpty()) {
                                                String details = multiErr(errorCodeList.getCauses()).getReadableDetails();
                                                logger.warn(String.format(
                                                        "failed to invoke after create image cache extensions (but still continue): %s",
                                                        details));
                                            }
                                            trigger.next();
                                        }
                                    });
                                }
                            });

                            done(new FlowDoneHandler(completion, chain) {
                                @Override
                                public void handle(Map data) {
                                    completion.success(ImageCacheInventory.valueOf(vo));
                                    chain.next();
                                }
                            });

                            error(new FlowErrorHandler(completion, chain) {
                                @Override
                                public void handle(ErrorCode errCode, Map data) {
                                    completion.fail(errCode);
                                    chain.next();
                                }
                            });
                        }
                    }).start();
                }

                private void checkEncryptImageCache(ImageCacheInventory inventory, final SyncTaskChain chain, String hostUuid) {
                    List<AfterCreateImageCacheExtensionPoint> extensionList = pluginRgty.getExtensionList(AfterCreateImageCacheExtensionPoint.class);

                    if (extensionList.isEmpty()) {
                        completion.success(inventory);
                        chain.next();
                        return;
                    }

                    extensionList.forEach(ext -> ext.checkEncryptImageCache(hostUuid, inventory, new Completion(chain) {
                        @Override
                        public void success() {
                            completion.success(inventory);
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                            chain.next();
                        }
                    }));
                }

                @Override
                public void run(final SyncTaskChain chain) {
                    SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
                    q.add(ImageCacheVO_.primaryStorageUuid, SimpleQuery.Op.EQ, self.getUuid());
                    q.add(ImageCacheVO_.imageUuid, SimpleQuery.Op.EQ, image.getUuid());
                    ImageCacheVO cache = q.find();
                    if (cache == null) {
                        doDownload(chain);
                        return;
                    }

                    SharedBlockKvmCommands.CheckBitsCmd cmd = new SharedBlockKvmCommands.CheckBitsCmd();
                    cmd.vgUuid = self.getUuid();
                    cmd.path = primaryStorageInstallPath;

                    new Do().go(CHECK_BITS_PATH, cmd, CheckBitsRsp.class, new ReturnValueCompletion<CheckBitsRsp>(completion, chain) {
                        @Override
                        public void success(CheckBitsRsp rsp) {
                            if (rsp.existing) {
                                checkEncryptImageCache(ImageCacheInventory.valueOf(cache), chain, cmd.hostUuid);
                                return;
                            }

                            // the image is removed on the host
                            // delete the cache object and re-download it
                            SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
                            q.add(ImageCacheVO_.primaryStorageUuid, SimpleQuery.Op.EQ, self.getUuid());
                            q.add(ImageCacheVO_.imageUuid, SimpleQuery.Op.EQ, image.getUuid());
                            ImageCacheVO vo = q.find();

                            IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                            imsg.setDiskSize(vo.getSize());
                            imsg.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
                            bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, vo.getPrimaryStorageUuid());
                            bus.send(imsg);

                            dbf.remove(vo);
                            doDownload(chain);
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                            chain.next();
                        }
                    });
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }
            });
        }
    }

    private BackupStorageSharedBlockKvmDownloader getBackupStorageKvmDownloader(String backupStorageUuid) {
        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.select(BackupStorageVO_.type);
        q.add(BackupStorageVO_.uuid, SimpleQuery.Op.EQ, backupStorageUuid);
        String bsType = q.findValue();

        for (BackupStorageSharedBlockKvmFactory f : pluginRgty.getExtensionList(BackupStorageSharedBlockKvmFactory.class)) {
            if (bsType.equals(f.getBackupStorageType())) {
                return f.createDownloader(getSelfInventory(), backupStorageUuid);
            }
        }
        throw new CloudRuntimeException(String.format("cannot find any BackupStorageKvmFactory for the type[%s]", bsType));
    }

    private BackupStorageSharedBlockKvmUploader getBackupStorageKvmUploader(String backupStorageUuid) {
        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.select(BackupStorageVO_.type);
        q.add(BackupStorageVO_.uuid, SimpleQuery.Op.EQ, backupStorageUuid);
        String bsType = q.findValue();

        if (bsType == null) {
            throw new OperationFailureException(operr("cannot find backup storage[uuid:%s]", backupStorageUuid));
        }

        for (BackupStorageSharedBlockKvmFactory f : pluginRgty.getExtensionList(BackupStorageSharedBlockKvmFactory.class)) {
            if (bsType.equals(f.getBackupStorageType())) {
                return f.createUploader(getSelfInventory(), backupStorageUuid);
            }
        }

        throw new CloudRuntimeException(String.format("cannot find any BackupStorageKvmFactory for the type[%s]", bsType));
    }

    // TODO(weiw): move it separately
    class Do {
        private KvmAgentCommandDispatcher dispatcher;

        Do(String huuid) {
            dispatcher = new KvmAgentCommandDispatcher(self == null ? null : self.getUuid(), huuid);
        }

        Do(String huuid, String psUuid) {
            dispatcher = new KvmAgentCommandDispatcher(psUuid, huuid);
        }

        Do(VolumeInventory volumeInventory) {
            dispatcher = new KvmAgentCommandDispatcher(self == null ? null : self.getUuid(), volumeInventory);
        }

        Do(String psUuid, boolean tmp) {
            dispatcher = new KvmAgentCommandDispatcher(psUuid, tmp);
        }

        Do() {
            dispatcher = new KvmAgentCommandDispatcher(self == null ? null : self.getUuid());
        }

        void go(String path, SharedBlockKvmCommands.AgentCmd cmd, ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp> completion) {
            dispatcher.go(path, cmd, completion);
        }

        <T extends SharedBlockKvmCommands.AgentRsp> void go(String path, SharedBlockKvmCommands.AgentCmd cmd, Class<T> rspType, ReturnValueCompletion<T> completion) {
            dispatcher.go(path, cmd, rspType, completion);
        }
    }

    public static String getVolumeProvision(String lvResourceUuid, String installPath) {
        Tuple t = Q.New(VolumeVO.class).select(VolumeVO_.uuid, VolumeVO_.primaryStorageUuid).eq(VolumeVO_.uuid, lvResourceUuid).findTuple();
        if (t == null) {
            t = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.volumeUuid, VolumeSnapshotVO_.primaryStorageUuid).eq(VolumeSnapshotVO_.uuid, lvResourceUuid).findTuple();
        }

        if (t == null) {
            return null;
        }

        String volumeUuid = t.get(0, String.class);
        String psUuid = t.get(1, String.class);

        if (VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.hasTag(volumeUuid, VolumeVO.class)) {
            String provisioningStrategy = VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.getTokenByResourceUuid(volumeUuid, VolumeVO.class, VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN);
            if (VolumeProvisioningStrategy.valueOf(provisioningStrategy).equals(VolumeProvisioningStrategy.ThinProvisioning)) {
                return VolumeProvisioningStrategy.ThinProvisioning.toString();
            }

            return null;
        }

        if (psUuid == null) {
            psUuid = getPrimaryStorageUuidByInstallPath(installPath);
        }

        if (VolumeSystemTags.PRIMARY_STORAGE_VOLUME_PROVISIONING_STRATEGY.hasTag(psUuid, PrimaryStorageVO.class)) {
            String provisioningStrategy = VolumeSystemTags.PRIMARY_STORAGE_VOLUME_PROVISIONING_STRATEGY.getTokenByResourceUuid(psUuid, PrimaryStorageVO.class, VolumeSystemTags.PRIMARY_STORAGE_VOLUME_PROVISIONING_STRATEGY_TOKEN);
            if (VolumeProvisioningStrategy.valueOf(provisioningStrategy).equals(VolumeProvisioningStrategy.ThinProvisioning)) {
                SystemTagCreator tagCreator = VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.newSystemTagCreator(volumeUuid);
                tagCreator.setTagByTokens(
                        map(
                                e(VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN,
                                        VolumeProvisioningStrategy.ThinProvisioning.toString())
                        )
                );
                tagCreator.inherent = false;
                tagCreator.create();

                return VolumeProvisioningStrategy.ThinProvisioning.toString();
            }
        }

        return null;
    }

    public static void callAgentHook(String path, final String hostUuid, SharedBlockKvmCommands.AgentCmd cmd, boolean noCheckStatus) {
        cmd.getAddons().putIfAbsent(THIN_PROVISIONING_INITIALIZE_SIZE_TAG_TOKEN, SharedBlockGlobalConfig.THIN_PROVISIONING_INITIALIZE_SIZE.value(Long.class));
        Optional.ofNullable(getBriefPvAllocateStrategy(cmd.vgUuid)).ifPresent(it -> cmd.addons.putIfAbsent(SharedBlockConstants.DEVICE_ALLOCATE_STRATEGY, it));

        if (cmd instanceof ProvisionSharedBlockVolumeCmd) {
            String provision = getVolumeProvision(((ProvisionSharedBlockVolumeCmd) cmd).getVolumeUuid(), ((ProvisionSharedBlockVolumeCmd) cmd).getInstallPath());
            if (provision != null && provision.equals(VolumeProvisioningStrategy.ThinProvisioning.toString())) {
                cmd.provisioning = VolumeProvisioningStrategy.ThinProvisioning.toString();
            }
        } else if (cmd instanceof DeleteBitsCmd) {
            if (srcf == null) {
                srcf = Platform.getComponentLoader().getComponent(ResourceConfigFacade.class);
            }
            ((DeleteBitsCmd) cmd).issueDiscards = srcf.getResourceConfigValue(SharedBlockGlobalConfig.DISCARD_VOLUME_WHEN_DELETING, cmd.primaryStorageUuid, String.class);
        }
    }

    static Map<String, String> getPvAllocateStrategy(String... vgUuids) {
        return getPvAllocateStrategy(vgUuids, true);
    }

    static Map<String, String> getBriefPvAllocateStrategy(String... vgUuids) {
        return getPvAllocateStrategy(vgUuids, false);
    }

    private static Map<String, String> getPvAllocateStrategy(String[] vgUuids, boolean displayNoneStrategy) {
        if (srcf == null) {
            srcf = Platform.getComponentLoader().getComponent(ResourceConfigFacade.class);
        }

        Map<String, String> ret = new HashMap<>();
        for (String vgUuid : vgUuids) {
            if (vgUuid == null) {
                logger.warn("unexpected vgUuid: null");
                continue;
            }

            String strategy = srcf.getResourceConfigValue(SharedBlockGlobalConfig.DEVICE_ALLOCATE_STRATEGY, vgUuid, String.class);
            if (displayNoneStrategy || !DeviceAllocateStrategy.none.toString().equals(strategy)) {
                ret.put(vgUuid, strategy);
            }
        }

        return ret.isEmpty() ? null : ret;
    }

    protected <T extends SharedBlockKvmCommands.AgentRsp> void httpCall(String path, final String hostUuid, SharedBlockKvmCommands.AgentCmd cmd, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        httpCall(path, hostUuid, cmd, false, rspType, completion);
    }

    public static void markVgInExclusiveLockIfNeed(SharedBlockKvmCommands.AgentCmd cmd, String path) {
        if (!acquireExVgLockPaths.contains(path)) {
            return;
        }
        synchronized (exclusiveLockOpInPrimaryStorage) {
            Cache<Integer, Boolean> cache = exclusiveLockOpInPrimaryStorage.computeIfAbsent(cmd.primaryStorageUuid, v ->
                    CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build());
            Integer hashValue = cmd.hashCode();
            cache.put(hashValue, true);
        }
    }

    public static void discardVgInExclusiveLockIfNeed(SharedBlockKvmCommands.AgentCmd cmd, String path) {
        if (!acquireExVgLockPaths.contains(path) || !exclusiveLockOpInPrimaryStorage.containsKey(cmd.primaryStorageUuid)) {
            return;
        }
        synchronized (exclusiveLockOpInPrimaryStorage) {
            Integer hashValue = cmd.hashCode();
            exclusiveLockOpInPrimaryStorage.get(cmd.primaryStorageUuid).invalidate(hashValue);
        }
    }

    private <T extends SharedBlockKvmCommands.AgentRsp> void httpCall(String path, final String hostUuid, SharedBlockKvmCommands.AgentCmd cmd, boolean noCheckStatus, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        if (cmd.vgUuid == null) {
            cmd.vgUuid = self.getUuid();
        }

        if (cmd.hostUuid == null) {
            cmd.hostUuid = hostUuid;
        }
        cmd.primaryStorageUuid = cmd.vgUuid;

        callAgentHook(path, hostUuid, cmd, noCheckStatus);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setPath(path);
        msg.setNoStatusCheck(noCheckStatus);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        markVgInExclusiveLockIfNeed(cmd, path);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                discardVgInExclusiveLockIfNeed(cmd, path);
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                final T rsp = r.toResponse(rspType);
                if (!rsp.success) {
                    completion.fail(operr("operation error, because:%s", rsp.error));
                    return;
                }

                if (rsp.totalCapacity != null && rsp.availableCapacity != null) {
                    new PrimaryStorageCapacityUpdater(cmd.vgUuid).run(new PrimaryStorageCapacityUpdaterRunnable() {
                        @Override
                        public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                            if (cap.getTotalCapacity() == 0 || cap.getAvailableCapacity() == 0) {
                                cap.setAvailableCapacity(rsp.availableCapacity);
                            }

                            cap.setTotalCapacity(rsp.totalCapacity);
                            cap.setTotalPhysicalCapacity(rsp.totalCapacity);
                            cap.setAvailablePhysicalCapacity(rsp.availableCapacity);

                            return cap;
                        }
                    });
                }

                if (rsp.lunCapacities != null) {
                    updateCapacity(rsp);
                }

                completion.success(rsp);
            }
        });
    }

    private static boolean isSharedBlockGroupPrimaryStorage(String psUuid) {
        return psUuid != null && Q.New(SharedBlockGroupVO.class)
                .eq(SharedBlockGroupVO_.uuid, psUuid)
                .isExists();
    }

    private void bulkActiveVolumes(List<VolumeInventory> sharedBlockVolumes, String hostUuid, LvmlockdLockingType lockingType, boolean killProcess, Completion completion) {
        List<String> volumeInstallPaths = new ArrayList<>();

        for (VolumeInventory v : sharedBlockVolumes) {
            if (v.getInstallPath() != null && !v.getInstallPath().equals("")) {
                volumeInstallPaths.add(v.getInstallPath());
            } else {
                volumeInstallPaths.add(makeInstallPath(v.getPrimaryStorageUuid(), v.getUuid()));
            }
        }

        bulkActiveVolumes(volumeInstallPaths, sharedBlockVolumes.get(0).getPrimaryStorageUuid(), hostUuid, lockingType, false, killProcess, completion);
    }

    private void bulkActiveVolumes(List<VolumeInventory> sharedBlockVolumes, String hostUuid, LvmlockdLockingType lockingType, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public void run(SyncTaskChain chain) {
                List<String> volumeInstallPaths = new ArrayList<>();

                for (VolumeInventory v : sharedBlockVolumes) {
                    if (v.getInstallPath() != null && !v.getInstallPath().equals("")) {
                        volumeInstallPaths.add(v.getInstallPath());
                    } else {
                        volumeInstallPaths.add(makeInstallPath(v.getPrimaryStorageUuid(), v.getUuid()));
                    }
                }

                bulkActiveVolumes(volumeInstallPaths, sharedBlockVolumes.get(0).getPrimaryStorageUuid(), hostUuid, lockingType, new Completion(chain) {
                    @Override
                    public void success() {
                        completion.success();
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return String.format("bulk-active-volumes-on-sharedblock-%s", sharedBlockVolumes.get(0).getPrimaryStorageUuid());
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            protected int getSyncLevel() {
                return SharedBlockGlobalConfig.VOLUME_ACTIVATE_SYNC_LEVEL.value(Integer.class);
            }
        });
    }

    private void updateActivateVolumeGc(String hostUuid, String volumeInstallPath, int lockType) {
        List<String> gcUuids = activateVolumeGCs.keySet().stream().filter(gcUuid -> activateVolumeGCs.get(gcUuid).contains(hostUuid)
                && activateVolumeGCs.get(gcUuid).contains(volumeInstallPath)).collect(Collectors.toList());

        if (gcUuids.isEmpty()) {
            return;
        }

        List<GarbageCollectorVO> gcVOs = Q.New(GarbageCollectorVO.class).in(GarbageCollectorVO_.uuid, gcUuids).list();
        for (GarbageCollectorVO gcVO : gcVOs) {
            String context = GarbageCollector.updateContext(gcVO.getUuid(), gcVO.getContext(), SharedBlockActivateVolumeGC.class, gc -> {
                gc.hostLockingTypeMap.remove(hostUuid);
            });
            activateVolumeGCs.put(gcVO.getUuid(), context);

            logger.debug(String.format("finish activating volume[installPath:%s] on host %s", volumeInstallPath, hostUuid));
        }
    }

    private void bulkActiveVolumes(List<String> volumeInstallPaths, String primaryStorageUuid, String hostUuid,
                                   LvmlockdLockingType lockingType, boolean deactivateWhenShared, boolean killProcess, Completion completion) {

        new While<>(volumeInstallPaths).step((String path, WhileCompletion completion1) -> {
            ActiveVolumeCmd cmd = new ActiveVolumeCmd();
            cmd.lockType = deactivateWhenShared ? lockingType.getValue() : getLockingTypeFromVolumeByInstallPath(path, lockingType).getValue();
            cmd.installPath = path;
            cmd.vgUuid = primaryStorageUuid;
            cmd.recursive = cmd.lockType != LvmlockdLockingType.NULL.getValue();
            cmd.killProcess = killProcess;
            new Do(hostUuid).go(SharedBlockKvmCommands.CHANGE_VOLUME_ACTIVE_PATH, cmd, ActivateRsp.class, new ReturnValueCompletion<ActivateRsp>(completion1) {
                @Override
                public void success(ActivateRsp activateRsp) {
                    updateActivateVolumeGc(hostUuid, path, cmd.lockType);
                    completion1.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion1.addError(errorCode);
                    completion1.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });
    }

    private void bulkActiveVolumes(List<String> volumeInstallPaths, String primaryStorageUuid, String hostUuid, LvmlockdLockingType lockingType, Completion completion) {
        bulkActiveVolumes(volumeInstallPaths, primaryStorageUuid, hostUuid, lockingType, false, false, completion);
    }

    private LvmlockdLockingType getLockingTypeFromVolume(VolumeInventory volume, LvmlockdLockingType lockingType) {
        return getLockingTypeFromVolume(volume.isShareable(), lockingType);
    }

    private LvmlockdLockingType getLockingTypeFromVolumeByInstallPath(String volumeInstallPath, LvmlockdLockingType lockingType) {
        Boolean isShared = Q.New(VolumeVO.class).select(VolumeVO_.isShareable).eq(VolumeVO_.installPath, volumeInstallPath).findValue();
        if (isShared == null) {
            String snapVolumeuuid = Q.New(VolumeSnapshotVO.class)
                    .select(VolumeSnapshotVO_.volumeUuid)
                    .eq(VolumeSnapshotVO_.primaryStorageInstallPath, volumeInstallPath)
                    .findValue();

            if (snapVolumeuuid != null ) {
                isShared = Q.New(VolumeVO.class).select(VolumeVO_.isShareable).eq(VolumeVO_.uuid, snapVolumeuuid).findValue();
            } else {
                logger.debug(String.format("can not get isShared of volume[installPath: %s], " +
                        "use original locking type[%s]", volumeInstallPath, lockingType));
                return lockingType;
            }
        }
        return getLockingTypeFromVolume(isShared, lockingType);
    }

    private LvmlockdLockingType getLockingTypeFromVolume(String volumeUuid, LvmlockdLockingType lockingType) {
        Boolean isShared = Q.New(VolumeVO.class).select(VolumeVO_.isShareable).eq(VolumeVO_.uuid, volumeUuid).findValue();
        if (isShared == null) {
            String snapVolumeuuid = Q.New(VolumeSnapshotVO.class)
                    .select(VolumeSnapshotVO_.volumeUuid)
                    .eq(VolumeSnapshotVO_.uuid, volumeUuid)
                    .findValue();

            if (snapVolumeuuid != null ) {
                isShared = Q.New(VolumeVO.class).select(VolumeVO_.isShareable).eq(VolumeVO_.uuid, snapVolumeuuid).findValue();
            } else {
                logger.debug(String.format("can not get isShared of volume[uuid: %s], " +
                        "use original locking type[%s]", volumeUuid, lockingType));
                return lockingType;
            }
        }
        return getLockingTypeFromVolume(isShared, lockingType);
    }

    private LvmlockdLockingType getLockingTypeFromVolume(Boolean isShared, LvmlockdLockingType lockingType) {
        if (!isShared) {
            return lockingType;
        }

        return LvmlockdLockingType.SHARE;
    }

    private void activeVolume(VolumeInventory volume, String hostUuid, LvmlockdLockingType lockingType) {
        activeVolume(volume, hostUuid, getLockingTypeFromVolume(volume.isShareable(), lockingType), true);
    }

    private void activeVolume(VolumeInventory volume, String hostUuid, LvmlockdLockingType lockingType, boolean recursive, Completion completion) {
        SharedBlockKvmCommands.ActiveVolumeCmd cmd = new SharedBlockKvmCommands.ActiveVolumeCmd();
        cmd.lockType = getLockingTypeFromVolume(volume, lockingType).getValue();
        cmd.recursive = recursive;
        cmd.installPath = volume.getInstallPath();
        if (cmd.installPath == null || cmd.installPath.equals("")) {
            cmd.installPath = makeInstallPath(volume.getPrimaryStorageUuid(), volume.getUuid());
        }
        cmd.vgUuid = volume.getPrimaryStorageUuid();

        new Do(hostUuid).go(SharedBlockKvmCommands.CHANGE_VOLUME_ACTIVE_PATH, cmd, ActivateRsp.class, new ReturnValueCompletion<ActivateRsp>(completion) {
            @Override
            public void success(ActivateRsp activateRsp) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void activeVolume(VolumeInventory volume, String hostUuid, LvmlockdLockingType lockingType, boolean recursive) {
        FutureCompletion completion = new FutureCompletion(null);
        activeVolume(volume, hostUuid, lockingType, recursive, completion);

        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    private void activeVolumeByInstallPath(String volumeInstallPath, String primaryStorageUuid, String hostUuid, LvmlockdLockingType lockingType, Boolean recursive, Completion completion) {
        ActiveVolumeCmd cmd = new ActiveVolumeCmd();
        cmd.lockType = getLockingTypeFromVolumeByInstallPath(volumeInstallPath, lockingType).getValue();
        cmd.installPath = volumeInstallPath;
        cmd.vgUuid = primaryStorageUuid;
        cmd.recursive = recursive;

        new Do(hostUuid).go(SharedBlockKvmCommands.CHANGE_VOLUME_ACTIVE_PATH, cmd, ActivateRsp.class, new ReturnValueCompletion<ActivateRsp>(completion) {
            @Override
            public void success(ActivateRsp activateRsp) {
                updateActivateVolumeGc(hostUuid, cmd.installPath, cmd.lockType);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void activeVolume(String volumeUuid, String primaryStorageUuid, String hostUuid, LvmlockdLockingType lockingType, Boolean recursive, Completion completion) {
        String installPath = Q.New(VolumeVO.class).select(VolumeVO_.installPath).eq(VolumeVO_.uuid, volumeUuid).findValue();
        ActiveVolumeCmd cmd = new ActiveVolumeCmd();
        cmd.lockType = getLockingTypeFromVolume(volumeUuid, lockingType).getValue();
        cmd.installPath = installPath == null ? makeInstallPath(primaryStorageUuid, volumeUuid) : installPath;
        cmd.vgUuid = primaryStorageUuid;
        cmd.recursive = recursive;

        new Do(hostUuid).go(SharedBlockKvmCommands.CHANGE_VOLUME_ACTIVE_PATH, cmd, ActivateRsp.class, new ReturnValueCompletion<ActivateRsp>(completion) {
            @Override
            public void success(ActivateRsp activateRsp) {
                updateActivateVolumeGc(hostUuid, cmd.installPath, cmd.lockType);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void activeVolume(String volumeUuid, String primaryStorageUuid, String hostUuid, LvmlockdLockingType lockingType, Completion completion) {
        activeVolume(volumeUuid, primaryStorageUuid, hostUuid, lockingType, true, completion);
    }

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) {
    }

    @Override
    public void preAttachIsoExtensionPoint(KVMHostInventory host, KVMAgentCommands.AttachIsoCmd acmd) {
        if (!acmd.iso.getPath().startsWith(SHARED_BLOCK_INSTALL_PATH_SCHEME)) {
            return;
        }
        acmd.iso.setPath(convertInstallPathToAbsolute(acmd.iso.getPath()));

        FutureCompletion completion = new FutureCompletion(null);
        ActiveVolumeCmd cmd = new ActiveVolumeCmd();
        cmd.lockType = LvmlockdLockingType.SHARE.getValue();
        cmd.installPath = acmd.iso.getPath();
        cmd.vgUuid = getPrimaryStorageUuidByAbsolutePath(acmd.iso.getPath());

        new Do(host.getUuid()).go(SharedBlockKvmCommands.CHANGE_VOLUME_ACTIVE_PATH, cmd, ActivateRsp.class, new ReturnValueCompletion<ActivateRsp>(completion) {
            @Override
            public void success(ActivateRsp activateRsp) {
                updateActivateVolumeGc(host.getUuid(), cmd.installPath, cmd.lockType);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });

        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    private void activateIso(VmInstanceSpec spec, Completion completion) {
        List<VmInstanceSpec.CdRomSpec> cdRomSpecs = spec.getCdRomSpecs();
        if (cdRomSpecs.isEmpty()) {
            completion.success();
            return;
        }

        List<String> isoUuids = spec.getCdRomSpecs().stream().map(VmInstanceSpec.CdRomSpec::getImageUuid).filter(Objects::nonNull).collect(Collectors.toList());
        if (isoUuids.isEmpty()) {
            completion.success();
            return;
        }

        Set<String> psUuids = cdRomSpecs.stream().map(VmInstanceSpec.CdRomSpec::getPrimaryStorageUuid)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (psUuids.size() != 1) {
            completion.fail(operr("VM[uuid:%s] has multiple ISOs from different primary storage: %s",
                    spec.getVmInventory().getUuid(), String.join(",", psUuids)));
            return;

        }

        final String psUuid = new ArrayList<>(psUuids).get(0);
        final List<String> installPaths = cdRomSpecs.stream()
                .filter(it -> it.getInstallPath() != null && isSharedBlockGroupPrimaryStorage(it.getPrimaryStorageUuid()))
                .map(VmInstanceSpec.CdRomSpec::getInstallPath)
                .collect(Collectors.toList());
        final String hostUuid = spec.getDestHost().getUuid();
        bulkActiveVolumes(installPaths, psUuid, hostUuid, LvmlockdLockingType.SHARE, completion);
        logger.debug(String.format("Activate ISO for vm[uuid:%s] success",
                spec.getVmInventory().getUuid()));
    }

    private void activateVolumes(VmInstanceSpec spec, Completion completion) {
        List<VolumeInventory> vols = new ArrayList<>();
        vols.add(spec.getDestRootVolume());
        vols.addAll(spec.getDestDataVolumes());
        List<VolumeInventory> sharedBlockVolumes = vols.stream()
                .filter(vol -> isSharedBlockGroupPrimaryStorage(vol.getPrimaryStorageUuid()))
                .collect(Collectors.toList());
        if (sharedBlockVolumes.isEmpty()) {
            completion.success();
            return;
        }

        bulkActiveVolumes(sharedBlockVolumes, spec.getDestHost().getUuid(), LvmlockdLockingType.EXCLUSIVE, completion);
        logger.debug(String.format("active shared block volumes for vm[uuid:%s] resource instantiate success",
                spec.getVmInventory().getUuid()));
    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        if (spec.getCurrentVmOperation().equals(VmInstanceConstant.VmOperation.ChangeImage)) {
            completion.success();
            return;
        }

        List<VolumeInventory> vols = new ArrayList<>();
        vols.add(spec.getDestRootVolume());
        vols.addAll(spec.getDestDataVolumes());
        List<VolumeInventory> sharedBlockVolumes = vols.stream()
                .filter(vol -> isSharedBlockGroupPrimaryStorage(vol.getPrimaryStorageUuid()))
                .collect(Collectors.toList());

        if (sharedBlockVolumes.isEmpty()) {
            completion.success();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("shared-block-instantiate-resource-for-vm-%s", spec.getVmInventory().getUuid()));

        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                activateVolumes(spec, new Completion(trigger) {
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

        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                activateIso(spec, new Completion(trigger) {
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

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        List<VolumeInventory> vols = new ArrayList<>();
        vols.add(spec.getDestRootVolume());
        vols.addAll(spec.getDestDataVolumes());
        List<VolumeInventory> sharedBlockVolumes = vols.stream()
                .filter(vol -> vol != null && isSharedBlockGroupPrimaryStorage(vol.getPrimaryStorageUuid()))
                .collect(Collectors.toList());

        if (sharedBlockVolumes.isEmpty()) {
            completion.success();
            return;
        }

        String hostUuid = spec.getDestHost() != null ? spec.getDestHost().getUuid() : spec.getVmInventory().getHostUuid();
        if (hostUuid == null) {
            hostUuid = spec.getVmInventory().getLastHostUuid();
        }

        if (hostUuid == null) {
            PrimaryStorageVO psVO = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, sharedBlockVolumes.get(0).getPrimaryStorageUuid()).find();
            List<HostInventory>hostUuids = primaryStorageFactory.getConnectedHostsForOperation(
                    PrimaryStorageInventory.valueOf(psVO), 0, 0, false);
            if (hostUuids != null && !hostUuids.isEmpty()) {
                hostUuid = hostUuids.get(0).getUuid();
            }
        }


        if (hostUuid == null) {
            logger.debug(String.format("can not get host for shared volume of vm[uuid:%s], skip to release vm resources",
                    spec.getVmInventory().getUuid()));
            completion.success();
            return;
        }

        bulkActiveVolumes(sharedBlockVolumes, hostUuid, LvmlockdLockingType.NULL, new Completion(completion) {
            @Override
            public void success() {
                completion.success();
                logger.debug(String.format("deactivate shared block volumes for vm[uuid:%s] resource release success",
                        spec.getVmInventory().getUuid()));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                // vm stop is triggered by host or ps deletion ignore the result
                if (spec.isIgnoreResourceReleaseFailure()) {
                    completion.success();
                    return;
                }

                completion.fail(errorCode);
            }
        });

    }

    @Override
    public void releaseVmResource(VmInstanceSpec spec, Completion completion) {
        preReleaseVmResource(spec, completion);
    }

    @Override
    public void preAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        boolean exists = Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, volume.getUuid())
                .eq(VolumeVO_.format, VolumeConstant.VOLUME_FORMAT_QCOW2)
                .notEq(VolumeVO_.isShareable, 0)
                .isExists();
        if (exists) {
            completion.fail(
                    operr("QCow2 shared volume[uuid:%s] is not supported", volume.getUuid())
            );
            return;
        }

        if (!isSharedBlockGroupPrimaryStorage(volume.getPrimaryStorageUuid()) ||
                vm.getHostUuid() == null) {
            completion.success();
            return;
        }

        if (VmInstanceState.Stopped.toString().equals(vm.getState())) {
            logger.debug(String.format("vm %s is stopped but has host uuid %s, skip to active volume %s", vm.getUuid(), vm.getHostUuid(), volume.getUuid()));
            completion.success();
            return;
        }

        activeVolume(volume, vm.getHostUuid(), LvmlockdLockingType.EXCLUSIVE, true, new Completion(completion) {
            @Override
            public void success() {
                logger.debug(String.format("active shared block volumes for attach volume[uuid:%s] to vm[uuid:%s] success",
                        volume.getUuid(), vm.getUuid()));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void afterInstantiateVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        if (!isSharedBlockGroupPrimaryStorage(volume.getPrimaryStorageUuid()) ||
                vm.getHostUuid() == null) {
            completion.success();
            return;
        }

        activeVolume(volume, vm.getHostUuid(), LvmlockdLockingType.EXCLUSIVE, true, new Completion(completion) {
            @Override
            public void success() {
                logger.debug(String.format("active shared block volumes for after instantiate volume[uuid:%s] to vm[uuid:%s] success",
                        volume.getUuid(), vm.getUuid()));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void beforeAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Map data) {
    }

    @Override
    public void afterAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {
    }

    @Override
    public void failedToAttachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode, Map data) {
        if (!isSharedBlockGroupPrimaryStorage(volume.getPrimaryStorageUuid()) ||
                vm.getHostUuid() == null) {
            return;
        }

        activeVolume(volume, vm.getHostUuid(), LvmlockdLockingType.NULL, false);
        logger.debug(String.format("deactivate shared block volumes for failed attach volume[uuid:%s] to vm[uuid:%s] success",
                volume.getUuid(), vm.getUuid()));
    }

    @Override
    public void afterDetachVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        if (!isSharedBlockGroupPrimaryStorage(volume.getPrimaryStorageUuid()) ||
                vm.getHostUuid() == null) {
            completion.success();
            return;
        }

        activeVolume(volume, vm.getHostUuid(), LvmlockdLockingType.NULL, false, completion);
        logger.debug(String.format("deactivate shared block volumes for detach volume[uuid:%s] from vm[uuid:%s] success",
                volume.getUuid(), vm.getUuid()));
    }

    @Override
    public void failedToDetachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode) {
        if (!isSharedBlockGroupPrimaryStorage(volume.getPrimaryStorageUuid()) ||
                vm.getHostUuid() == null) {
            return;
        }

        activeVolume(volume, vm.getHostUuid(), LvmlockdLockingType.EXCLUSIVE);
        logger.debug(String.format("active shared block volumes for failed detach volume[uuid:%s] from vm[uuid:%s] success",
                volume.getUuid(), vm.getUuid()));
    }

    private List<String> getAttachedIsoInstallPaths(String psUuid, String vmInstanceUuid) {
        List<String> vmIsoUuids = IsoOperator.getIsoUuidByVmUuid(vmInstanceUuid);
        if (vmIsoUuids == null || vmIsoUuids.isEmpty()) {
            return null;
        }
        return vmIsoUuids.stream()
                .map(uuid -> makeInstallPath(psUuid, uuid))
                .collect(Collectors.toList());
    }

    private void activateIsosForMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        if (!isSharedBlockGroupPrimaryStorage(inv.getRootVolume().getPrimaryStorageUuid())) {
            return;
        }

        List<String> attachedIsoInstallPaths = getAttachedIsoInstallPaths(
                inv.getRootVolume().getPrimaryStorageUuid(), inv.getUuid());

        if (attachedIsoInstallPaths == null || attachedIsoInstallPaths.isEmpty()) {
            return;
        }

        logger.debug(String.format("prepare isos[install path: %s] for migrate vm[uuid: %s]",attachedIsoInstallPaths, inv.getUuid()));
        FutureCompletion completion = new FutureCompletion(null);
        bulkActiveVolumes(attachedIsoInstallPaths, inv.getRootVolume().getPrimaryStorageUuid(), destHostUuid, LvmlockdLockingType.SHARE, completion);
        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    // TODO(weiw) Optimize SQL
    private List<VolumeInventory> getVmInstanceAttachedSharedVolumes(String vmInstanceUuid) {
        List<String> volumeUuids = Q.New(ShareableVolumeVmInstanceRefVO.class)
            .select(ShareableVolumeVmInstanceRefVO_.volumeUuid)
            .eq(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid, vmInstanceUuid)
            .listValues();
        if (volumeUuids == null || volumeUuids.isEmpty()) {
            return new ArrayList<>();
        } else {
            List<VolumeVO> volumeVOS = Q.New(VolumeVO.class).in(VolumeVO_.uuid, volumeUuids).list();
            volumeVOS = volumeVOS.stream()
                    .filter(vo -> isSharedBlockGroupPrimaryStorage(vo.getPrimaryStorageUuid()))
                    .collect(Collectors.toList());
            return VolumeInventory.valueOf(volumeVOS);
        }
    }

    @Override
    public void preCopyVolumes(List<VolumeInventory> oldVolumes, List<VolumeInventory> newVolumes, Map<String, String> old2NewVolumeUuids, String dstHostUuid, Completion completion) {
        List <VolumeInventory> sharedBlockVolumes = newVolumes.stream()
                .filter(vol -> isSharedBlockGroupPrimaryStorage(vol.getPrimaryStorageUuid()))
                .collect(Collectors.toList());

        if (sharedBlockVolumes.isEmpty()) {
            completion.success();
            return;
        }
        boolean isThinProvisioning  = VolumeProvisioningStrategy.ThinProvisioning.toString().equals(
                PrimaryStorageProvisioningStrategyGetter.get(sharedBlockVolumes.get(0).getPrimaryStorageUuid()));
        Map<String, Long> measuredNewVolumeRequiredSizes = new ConcurrentHashMap<>();
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-shared-block-for-migrate-volumes-to-host-%s", dstHostUuid));
        chain.then(new NoRollbackFlow() {
            String __name__ = "measure-old-volumes-size";

            @Override
            public boolean skip(Map data) {
                return !isThinProvisioning;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(oldVolumes).step((volumeInventory, whileCompletion) -> {
                    EstimateVolumeTemplateSizeMsg emsg = new EstimateVolumeTemplateSizeMsg();
                    emsg.setVolumeUuid(volumeInventory.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(emsg, VolumeConstant.SERVICE_ID, emsg.getVolumeUuid());
                    bus.send(emsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                whileCompletion.addError(reply.getError());
                                whileCompletion.allDone();
                                return;
                            }
                            EstimateVolumeTemplateSizeReply r = (EstimateVolumeTemplateSizeReply) reply;
                            if (r.getActualSize() < r.getSize()) {
                                measuredNewVolumeRequiredSizes.put(old2NewVolumeUuids.get(volumeInventory.getUuid()), r.getActualSize());
                            } else {
                                logger.debug(String.format("unable to measure actual size for %s, maybe it's a full " +
                                        "or thick provision volume, use 3x thin.provisioning.initialize.size", volumeInventory.getInstallPath()));

                                measuredNewVolumeRequiredSizes.put(old2NewVolumeUuids.get(volumeInventory.getUuid()),
                                        Math.min(3*Long.parseLong(SharedBlockGlobalConfig.THIN_PROVISIONING_INITIALIZE_SIZE.value()), r.getSize()));
                            }

                            whileCompletion.done();
                        }
                    });
                }, 5).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            trigger.fail(errorCodeList.getCauses().get(0));
                            return;
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "extend-migrate-target-if-need";

            @Override
            public boolean skip(Map data) {
                return !isThinProvisioning;
            }
            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(sharedBlockVolumes).step((volumeInventory, whileCompletion) -> {
                    if (!measuredNewVolumeRequiredSizes.containsKey(volumeInventory.getUuid())) {
                        logger.debug(String.format("skip extend dst volume %s", volumeInventory.getInstallPath()));
                        whileCompletion.done();
                        return;
                    }

                    ExtendLogicalVolumeCmd cmd = new ExtendLogicalVolumeCmd();
                    cmd.requiredSize = measuredNewVolumeRequiredSizes.get(volumeInventory.getUuid());
                    cmd.volumeUuid = volumeInventory.getUuid();
                    cmd.destPath = volumeInventory.getInstallPath();
                    cmd.vgUuid = volumeInventory.getPrimaryStorageUuid();
                    new Do(dstHostUuid).go(EXTEND_LOGICAL_VOLUME_PATH, cmd, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(whileCompletion) {
                        @Override
                        public void success(SharedBlockKvmCommands.AgentRsp rsp) {
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            whileCompletion.addError(errorCode);
                            whileCompletion.allDone();
                        }
                    });
                }, 5).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            trigger.fail(errorCodeList.getCauses().get(0));
                            return;
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bulkActiveVolumes(sharedBlockVolumes, dstHostUuid, LvmlockdLockingType.EXCLUSIVE, new Completion(trigger) {
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
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void afterCopyVolumes(List<VolumeInventory> oldVolumes, String srcHostUuid) {
        List <VolumeInventory> sharedBlockVolumes = oldVolumes.stream()
                .filter(vol -> isSharedBlockGroupPrimaryStorage(vol.getPrimaryStorageUuid()))
                .collect(Collectors.toList());

        if (sharedBlockVolumes.isEmpty()) {
            return;
        }

        bulkActiveVolumes(sharedBlockVolumes, srcHostUuid, LvmlockdLockingType.NULL, new NopeCompletion());
    }

    @Override
    public void preMigrateVolumes(List<VolumeInventory> oldVolumes, String srcHostUuid, String dstHostUuid, Completion completion) {
        List<VolumeInventory> sharedBlockVolumes = oldVolumes.stream()
                .filter(vol -> isSharedBlockGroupPrimaryStorage(vol.getPrimaryStorageUuid()))
                .collect(Collectors.toList());

        if (sharedBlockVolumes.isEmpty()) {
            completion.success();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-shared-block-for-blkmigrate-vm-%s", sharedBlockVolumes.get(0).getVmInstanceUuid()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bulkActiveVolumes(sharedBlockVolumes, srcHostUuid, LvmlockdLockingType.SHARE, new Completion(trigger) {
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
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bulkActiveVolumes(sharedBlockVolumes, dstHostUuid, LvmlockdLockingType.SHARE, new Completion(trigger) {
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
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void failedToMigrateVolumes(List<VolumeInventory> oldVolumes, String srcHostUuid, String dstHostUuid, Completion completion) {
        List<VolumeInventory> sharedBlockVolumes = oldVolumes.stream()
                .filter(vol -> isSharedBlockGroupPrimaryStorage(vol.getPrimaryStorageUuid()))
                .collect(Collectors.toList());

        if (sharedBlockVolumes.isEmpty()) {
            completion.success();
            return;
        }
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("faild-to-migrate-volumes-for-vm-%s", sharedBlockVolumes.get(0).getVmInstanceUuid()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bulkActiveVolumes(sharedBlockVolumes, dstHostUuid, LvmlockdLockingType.NULL, new Completion(completion) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bulkActiveVolumes(sharedBlockVolumes, srcHostUuid, LvmlockdLockingType.EXCLUSIVE, new Completion(completion) {
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
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void afterMigrateVolumes(List<VolumeInventory> oldVolumes, String srcHostUuid, String dstHostUuid, Completion completion) {
        List<VolumeInventory> sharedBlockVolumes = oldVolumes.stream()
                .filter(vol -> isSharedBlockGroupPrimaryStorage(vol.getPrimaryStorageUuid()))
                .collect(Collectors.toList());

        if (sharedBlockVolumes.isEmpty()) {
            completion.success();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("sharedblock-after-blkmigrate-vm-%s", sharedBlockVolumes.get(0).getVmInstanceUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = String.format("deactive-volumes-for-vm-on-src-host-%s", srcHostUuid);

            @Override
            public void run(FlowTrigger trigger, Map data) {
                bulkActiveVolumes(sharedBlockVolumes, srcHostUuid, LvmlockdLockingType.NULL, new Completion(trigger) {
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
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("exclusive-active-volumes-for-vm-on-dest-host-%s", dstHostUuid);
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bulkActiveVolumes(sharedBlockVolumes, dstHostUuid, LvmlockdLockingType.EXCLUSIVE, new Completion(trigger) {
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
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).start();
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        activateIsosForMigrateVm(inv, destHostUuid);

        List<VolumeInventory> sharedBlockVolumes = inv.getAllVolumes().stream()
                .filter(vol -> isSharedBlockGroupPrimaryStorage(vol.getPrimaryStorageUuid()) && !VolumeType.Memory.toString().equals(vol.getType()))
                .collect(Collectors.toList());
        sharedBlockVolumes.addAll(getVmInstanceAttachedSharedVolumes(inv.getUuid()));

        if (sharedBlockVolumes.isEmpty()) {
            return;
        }

        FutureCompletion completion = new FutureCompletion(null);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-shared-block-for-migrate-vm-%s", inv.getUuid()));
        chain.then(new Flow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bulkActiveVolumes(sharedBlockVolumes, inv.getHostUuid(), LvmlockdLockingType.SHARE, new Completion(completion) {
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

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                bulkActiveVolumes(sharedBlockVolumes, inv.getHostUuid(), LvmlockdLockingType.EXCLUSIVE, new Completion(completion) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.rollback();
                    }
                });
            }
        }).then(new Flow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bulkActiveVolumes(sharedBlockVolumes, destHostUuid, LvmlockdLockingType.SHARE, new Completion(completion) {
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

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                bulkActiveVolumes(sharedBlockVolumes, destHostUuid, LvmlockdLockingType.NULL, new Completion(completion) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.rollback();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();

        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        List<VolumeInventory> sharedBlockVolumes = inv.getAllVolumes().stream()
                .filter(vol -> isSharedBlockGroupPrimaryStorage(vol.getPrimaryStorageUuid()))
                .collect(Collectors.toList());

        if (sharedBlockVolumes.isEmpty()) {
            return;
        }

        Map<VolumeInventory, LinkedHashMap<String, String>> failedResults = sharedBlockVolumes.stream().collect(Collectors.toConcurrentMap(sharedBlockVolume -> sharedBlockVolume, v -> {
            LinkedHashMap<String, String> hostLockingTypeMap = new LinkedHashMap<>();
            hostLockingTypeMap.put(srcHostUuid, LvmlockdLockingType.NULL.toString());
            hostLockingTypeMap.put(inv.getHostUuid(), LvmlockdLockingType.EXCLUSIVE.toString());
            return hostLockingTypeMap;
        }));

        FutureCompletion completion = new FutureCompletion(null);
        new While<>(sharedBlockVolumes).step((volumeInv, compl) -> {
            activeVolumeByInstallPath(volumeInv.getInstallPath(), volumeInv.getPrimaryStorageUuid(), srcHostUuid, LvmlockdLockingType.NULL, false, new Completion(compl) {
                @Override
                public void success() {
                    failedResults.get(volumeInv).remove(srcHostUuid);
                    activeVolumeByInstallPath(volumeInv.getInstallPath(), volumeInv.getPrimaryStorageUuid(), inv.getHostUuid(), LvmlockdLockingType.EXCLUSIVE, false, new Completion(compl) {
                        @Override
                        public void success() {
                            failedResults.remove(volumeInv);
                            compl.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.debug(String.format("failed to activate volume[installPath:%s] on host[uuid:%s], because %s", volumeInv.getInstallPath(), inv.getUuid(), errorCode.getDetails()));
                            compl.addError(errorCode);
                            compl.done();
                        }
                    });
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.debug(String.format("failed to deactivate volume[installPath:%s] on host[uuid:%s], because %s", volumeInv.getInstallPath(), srcHostUuid, errorCode.getDetails()));
                    compl.addError(errorCode);
                    compl.done();
                }
            });

        }, 3).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });
        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            // save failed operation to gc
            for (Map.Entry<VolumeInventory, LinkedHashMap<String, String>> e : failedResults.entrySet()) {
                SharedBlockActivateVolumeGC gc = new SharedBlockActivateVolumeGC();
                gc.NAME = String.format("activate-shared-block-volume-%s-on-host[seq:%s]", e.getKey().getInstallPath(), e.getValue().toString());
                gc.primaryStorageUuid = e.getKey().getPrimaryStorageUuid();
                gc.hypervisorType = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(e.getKey().getFormat()).toString();
                gc.installPath = e.getKey().getInstallPath();
                gc.hostLockingTypeMap = e.getValue();
                gc.deduplicateSubmit(Math.min(SharedBlockGlobalConfig.GC_INTERVAL.value(Long.class), MAX_ACTIVATE_VOLUME_GC_INTERVAL_IN_SEC), TimeUnit.SECONDS);
            }
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        if (destHostUuid == null) {
            return;
        }

        List<VolumeInventory> sharedBlockVolumes = inv.getAllVolumes().stream()
                .filter(vol -> isSharedBlockGroupPrimaryStorage(vol.getPrimaryStorageUuid()))
                .collect(Collectors.toList());

        if (sharedBlockVolumes.isEmpty()) {
            return;
        }

        FutureCompletion completion = new FutureCompletion(null);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-shared-block-for-fail-migrate-vm-%s", inv.getUuid()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bulkActiveVolumes(sharedBlockVolumes, destHostUuid, LvmlockdLockingType.NULL, true, new Completion(completion) {
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
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bulkActiveVolumes(sharedBlockVolumes, inv.getHostUuid(), LvmlockdLockingType.EXCLUSIVE, new Completion(completion) {
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
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();

        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    @Override
    public void beforeSnapshotTake(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, Completion completion) {
        if (!isSharedBlockGroupPrimaryStorage(msg.getVolume().getPrimaryStorageUuid())) {
            completion.success();
            return;
        }

        boolean offlineFullSnapshot = msg.isFullSnapshot() && !cmd.isOnline();

        // If taking live full snapshot, first block stream volume and take top as full snapshot,
        // so all we need to do is create a new LV based qcow2 as the top
        if (offlineFullSnapshot) {
            cmd.setNewVolumeUuid(getUuid());
            cmd.setNewVolumeInstallPath(makeInstallPath(msg.getVolume().getPrimaryStorageUuid(), cmd.getNewVolumeUuid()));
            logger.debug(String.format("full snapshot required, volume[uuid: %s, install path: %s], " +
                            "snapshot[uuid: %s, install path: %s], new volume[uuid: %s, install path: %s]",
                    cmd.getVolumeUuid(), cmd.getVolumeInstallPath(),
                    getVolumeUuidByAbsolutePath(cmd.getInstallPath()), cmd.getInstallPath(),
                    cmd.getNewVolumeUuid(), cmd.getNewVolumeInstallPath()));
        } else {
            cmd.setNewVolumeUuid(getVolumeUuidByAbsolutePath(cmd.getInstallPath()));
            cmd.setNewVolumeInstallPath(cmd.getInstallPath());
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-shared-block-volume-%s-for-snapshot-%s", msg.getVolume().getUuid(), msg.getSnapshotName()));
        chain.then(new Flow() {
            String __name__= "create new lv for offline full snapshot";

            @Override
            public boolean skip(Map data) {
                return !offlineFullSnapshot;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                final SharedBlockKvmCommands.CreateEmptyVolumeCmd scmd = new SharedBlockKvmCommands.CreateEmptyVolumeCmd();
                scmd.installPath = cmd.getNewVolumeInstallPath();
                scmd.name = msg.getVolume().getName();
                scmd.size = msg.getVolume().getSize();
                scmd.volumeUuid = cmd.getVolumeUuid();
                scmd.vgUuid = msg.getVolume().getPrimaryStorageUuid();

                new Do(msg.getHostUuid()).go(SharedBlockKvmCommands.CREATE_EMPTY_VOLUME_PATH, scmd, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(trigger) {
                    @Override
                    public void success(SharedBlockKvmCommands.AgentRsp returnValue) {
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
                final SharedBlockKvmCommands.DeleteBitsCmd scmd = new SharedBlockKvmCommands.DeleteBitsCmd();
                scmd.vgUuid = msg.getVolume().getPrimaryStorageUuid();
                scmd.folder = false;
                scmd.path = cmd.getNewVolumeInstallPath();
                new Do(msg.getHostUuid()).go(SharedBlockKvmCommands.DELETE_BITS_PATH, scmd, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(completion) {
                    @Override
                    public void success(SharedBlockKvmCommands.AgentRsp returnValue) {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.rollback();
                    }
                });
            }
        }).then(new Flow() {
            String __name__= "create new volume";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                final SharedBlockKvmCommands.CreateEmptyVolumeCmd scmd = new SharedBlockKvmCommands.CreateEmptyVolumeCmd();
                scmd.installPath = cmd.getInstallPath();
                scmd.name = msg.getSnapshotName();
                scmd.size = msg.getVolume().getSize();
                scmd.volumeUuid = cmd.getNewVolumeUuid();
                scmd.vgUuid = msg.getVolume().getPrimaryStorageUuid();
                if (cmd.isOnline()) {
                    scmd.backingFile = cmd.getVolumeInstallPath();
                }

                new Do(msg.getHostUuid()).go(SharedBlockKvmCommands.CREATE_EMPTY_VOLUME_PATH, scmd, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(trigger) {
                    @Override
                    public void success(SharedBlockKvmCommands.AgentRsp returnValue) {
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
                final SharedBlockKvmCommands.DeleteBitsCmd scmd = new SharedBlockKvmCommands.DeleteBitsCmd();
                scmd.vgUuid = msg.getVolume().getPrimaryStorageUuid();
                scmd.folder = false;
                scmd.path = cmd.getInstallPath();
                new Do(msg.getHostUuid()).go(SharedBlockKvmCommands.DELETE_BITS_PATH, scmd, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(completion) {
                    @Override
                    public void success(SharedBlockKvmCommands.AgentRsp returnValue) {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.rollback();
                    }
                });
            }
        }).then(new Flow() {
            String __name__= "active existing volume";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                activeVolume(msg.getVolume().getUuid(), msg.getVolume().getPrimaryStorageUuid(), msg.getHostUuid(), LvmlockdLockingType.SHARE, new Completion(completion) {
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

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
            }
        }).then(new Flow() {
            String __name__= "active new volume";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                LvmlockdLockingType lock = LvmlockdLockingType.EXCLUSIVE;
                if (msg.isFullSnapshot()) {
                    lock = LvmlockdLockingType.SHARE;
                }
                activeVolumeByInstallPath(cmd.getInstallPath(), msg.getVolume().getPrimaryStorageUuid(), msg.getHostUuid(), lock, true, new Completion(completion) {
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

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                activeVolumeByInstallPath(cmd.getInstallPath(), msg.getVolume().getPrimaryStorageUuid(), msg.getHostUuid(), LvmlockdLockingType.NULL, false, new Completion(completion) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.rollback();
                    }
                });
            }
        }).then(new Flow() {
            String __name__= "active new volume if full";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (!msg.isFullSnapshot()){
                    trigger.next();
                    return;
                }
                activeVolume(cmd.getNewVolumeUuid(), msg.getVolume().getPrimaryStorageUuid(), msg.getHostUuid(), LvmlockdLockingType.EXCLUSIVE, new Completion(completion) {
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

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (!msg.isFullSnapshot()){
                    trigger.rollback();
                    return;
                }
                activeVolume(cmd.getNewVolumeUuid(), msg.getVolume().getPrimaryStorageUuid(), msg.getHostUuid(), LvmlockdLockingType.NULL, false, new Completion(completion) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.rollback();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                cmd.setInstallPath(convertInstallPathToAbsolute(cmd.getInstallPath()));
                cmd.setVolumeInstallPath(convertInstallPathToAbsolute(cmd.getVolumeInstallPath()));
                cmd.setNewVolumeInstallPath(
                        convertInstallPathToAbsolute(makeInstallPath(
                                msg.getVolume().getPrimaryStorageUuid(), cmd.getNewVolumeUuid())));
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void afterSnapshotTake(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, KVMAgentCommands.TakeSnapshotResponse rsp) {
        if (!isSharedBlockGroupPrimaryStorage(msg.getVolume().getPrimaryStorageUuid())) {
            return;
        }

        // TODO(weiw): need to deactivate snap too
        FutureCompletion completion = new FutureCompletion(msg);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("after-take-snapshot-%s-for-volume-%s", msg.getSnapshotName(), msg.getVolume().getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__= "shrink-snapShot";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (!SharedBlockGlobalConfig.SHRINK_SNAPSHOT.value(Boolean.class) || cmd.isFullSnapshot()) {
                    trigger.next();
                    return;
                }

                ShrinkSnapshotCmd scmd = new ShrinkSnapshotCmd();
                scmd.installPath = cmd.getVolumeInstallPath();
                scmd.vgUuid = msg.getVolume().getPrimaryStorageUuid();
                new Do(msg.getHostUuid()).go(SHRINK_SNAPSHOT_PATH, scmd, ShrinkSnapshotRsp.class, new ReturnValueCompletion<ShrinkSnapshotRsp>(completion) {
                    @Override
                    public void success(ShrinkSnapshotRsp reply) {
                        logger.debug(String.format("successfully shrink sblk snapshot[%s] from %s to %s ", cmd.getVolumeInstallPath(), reply.oldSize, reply.size));
                        rsp.setSize(reply.size);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("shrink sblk snapshot[%s] failed:%n%s",
                                cmd.getVolumeInstallPath(), errorCode.getReadableDetails()));
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__= "deactivate new volume";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmInstanceVO vo = null;
                if (msg.getVmUuid() != null) {
                    vo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmUuid()).find();
                }

                if (vo != null && (vo.getState().equals(VmInstanceState.Running) ||
                        vo.getState().equals(VmInstanceState.Paused))) {
                    trigger.next();
                    return;
                }

                activeVolumeByInstallPath(convertAbsolutePathToInstall(
                        rsp.getNewVolumeInstallPath()), msg.getVolume().getPrimaryStorageUuid(), msg.getHostUuid(), LvmlockdLockingType.NULL, false, new Completion(completion) {
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
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).start();

        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            logger.warn(String.format("shrink-snapshot-%s-failed", msg.getSnapshotName()));
        }
    }

    @Override
    public void afterSnapshotTakeFailed(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, KVMAgentCommands.TakeSnapshotResponse rsp, ErrorCode err) {
        if (!isSharedBlockGroupPrimaryStorage(msg.getVolume().getPrimaryStorageUuid())) {
            return;
        }

        deleteBits(cmd.getNewVolumeInstallPath(), false, msg.getHostUuid(), new Completion(msg) {
            @Override
            public void success() {
                logger.debug(String.format("successfully cleaned garbage volume[uuid: %s, install path: %s] for take snapshot on volume[%s]",
                        cmd.getNewVolumeUuid(), cmd.getNewVolumeInstallPath(), msg.getVolume().getUuid()));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (errorCode.isError(VolumeErrors.VOLUME_IN_USE)) {
                    logger.debug(String.format("can't delete volume[uuid:%s] right now, skip this GC job because it's in use", msg.getVolume().getUuid()));
                    return;
                }
                logger.debug(String.format("failed to clean garbage volume[uuid: %s, install path: %s] for failed taking snapshot on volume[%s], " +
                                "create gc job to clean garbage later", cmd.getNewVolumeUuid(), cmd.getNewVolumeInstallPath(), msg.getVolume().getUuid()));

                SharedBlockDeleteVolumeGC gc = new SharedBlockDeleteVolumeGC();
                gc.NAME = String.format("gc-shared-block-%s-snapshot-%s", msg.getVolume().getPrimaryStorageUuid(), cmd.getNewVolumeInstallPath());
                gc.primaryStorageUuid = msg.getVolume().getPrimaryStorageUuid();
                gc.hypervisorType = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getVolume().getFormat()).toString();
                gc.volume = msg.getVolume();
                gc.volume.setInstallPath(cmd.getNewVolumeInstallPath());
                gc.submit(SharedBlockGlobalConfig.GC_INTERVAL.value(Long.class), TimeUnit.SECONDS);
            }
        });
    }

    @Override
    public void beforeResizeVolume(VolumeVO volumeVO, long resize, VolumeType type, ResizeVolumeStruct struct, Completion completion) {
        if (volumeVO == null || !isSharedBlockGroupPrimaryStorage(volumeVO.getPrimaryStorageUuid())) {
            completion.success();
            return;
        }

        if (!struct.isVmRunning()) {
            completion.success();
            return;
        }

        List<HostInventory> hosts = primaryStorageFactory.getConnectedHostsForOperation(
                volumeVO.getPrimaryStorageUuid(), VolumeInventory.valueOf(volumeVO));
        SharedBlockKvmCommands.ResizeVolumeCmd cmd = new SharedBlockKvmCommands.ResizeVolumeCmd();
        cmd.installPath = volumeVO.getInstallPath();
        cmd.size = resize;
        cmd.vgUuid = volumeVO.getPrimaryStorageUuid();
        cmd.hostUuid = struct.getVmHostUuid() == null ? hosts.get(0).getUuid() : struct.getVmHostUuid();
        cmd.live = struct.isVmRunning();
        cmd.volumeUuid = volumeVO.getUuid();
        httpCall(RESIZE_VOLUME_PATH, cmd.hostUuid, cmd, SharedBlockKvmCommands.ResizeVolumeRsp.class, new ReturnValueCompletion<SharedBlockKvmCommands.ResizeVolumeRsp>(completion) {
            @Override
            public void success(SharedBlockKvmCommands.ResizeVolumeRsp rsp) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public static List<String> convertInstallPathsToAbsolute(List<String> installPaths) {
        if (CollectionUtils.isEmpty(installPaths)) {
            return installPaths;
        }

        List<String> absoluteInstallPaths = new ArrayList<>();
        installPaths.forEach(installPath -> absoluteInstallPaths.add(convertInstallPathToAbsolute(installPath)));
        return absoluteInstallPaths;
    }

    public static String convertInstallPathToAbsolute(String installPath) {
        if (!installPath.startsWith(SHARED_BLOCK_INSTALL_PATH_SCHEME)) {
            return installPath;
        }

        return installPath.replace(
                String.format("%s", SHARED_BLOCK_INSTALL_PATH_SCHEME),
                SHARED_BLOCK_ABSOLUTE_PATH_SCHEME);
    }

    public static String convertAbsolutePathToInstall(String path) {
        if (!path.startsWith(SHARED_BLOCK_ABSOLUTE_PATH_SCHEME)) {
            return path;
        }

        return path.replace(
                String.format("%s", SHARED_BLOCK_ABSOLUTE_PATH_SCHEME),
                SHARED_BLOCK_INSTALL_PATH_SCHEME);
    }

    public static String getVolumeUuidByAbsolutePath(String path) {
        // NOTE(weiw): assume format: /dev/{vgUuid}/{volumeUuid}
        return path.split("/")[3];
    }

    public static String getPrimaryStorageUuidByAbsolutePath(String path) {
        if (!path.startsWith(SHARED_BLOCK_ABSOLUTE_PATH_SCHEME)) {
            return null;
        }

        return path.split("/")[2];
    }

    public static String getPrimaryStorageUuidByPath(String path) {
        if (path.startsWith(SHARED_BLOCK_INSTALL_PATH_SCHEME) || path.startsWith(SHARED_BLOCK_ABSOLUTE_PATH_SCHEME)) {
            return path.split("/")[2];
        }

        return null;
    }

    public static String getPrimaryStorageUuidByInstallPath(String path) {
        if (!path.startsWith(SHARED_BLOCK_INSTALL_PATH_SCHEME)) {
            return null;
        }

        return path.split("/")[2];
    }

    public static String buildQcow2Options() {
        StringBuilder options = new StringBuilder(String.format(" -o cluster_size=%s ",
                SharedBlockGlobalConfig.QCOW2_CLUSTER_SIZE.value()));

        if (VALID_QCOW2_ALLOCATION.contains(SharedBlockGlobalConfig.QCOW2_ALLOCATION.value()) &&
                !SharedBlockGlobalConfig.QCOW2_ALLOCATION.value().equals("none")) {
            options.append(String.format(" -o preallocation=%s ", SharedBlockGlobalConfig.QCOW2_ALLOCATION.value()));
        }
        return options.toString();
    }

    public static boolean getFailIfNoPath() {
        return SharedBlockGlobalConfig.FAIL_IF_MULTIPATH_NO_PATH.value().equals("true");
    }

    @Override
    public void handle(AskInstallPathForNewSnapshotMsg msg, ReturnValueCompletion<AskInstallPathForNewSnapshotReply> completion) {
        AskInstallPathForNewSnapshotReply reply = new AskInstallPathForNewSnapshotReply();
        reply.setSnapshotInstallPath(makeInstallPath(msg.getVolumeInventory().getPrimaryStorageUuid(), msg.getSnapshotUuid()));
        completion.success(reply);
    }

    @Override
    public void handle(TakeSnapshotOnSharedBlockGroupPrimaryStorageMsg msg, ReturnValueCompletion<TakeSnapshotOnSharedBlockGroupPrimaryStorageReply> completion) {
        Set<String> srcHostUuids = primaryStorageFactory.getConnectedHostsForOperation(msg.getPrimaryStorageUuid())
                .stream().map(HostInventory::getUuid).collect(Collectors.toSet());
        if (srcHostUuids.isEmpty()) {
            completion.fail(operr("can not find any available host to take snapshot for volume[uuid: %s] on " +
                    "shared block group primary storage[uuid: %s]", msg.getVolumeUuid(), msg.getPrimaryStorageUuid()));
            return;
        }

        Set<String> dstHostUuids = primaryStorageFactory.getConnectedHostsForOperation(msg.getTargetPrimaryStorageUuid())
                .stream().map(HostInventory::getUuid).collect(Collectors.toSet());
        if (dstHostUuids.isEmpty()) {
            completion.fail(operr("can not find any available host to take snapshot for volume[uuid: %s] on " +
                    "shared block group primary storage[uuid: %s]", msg.getVolumeUuid(), msg.getTargetPrimaryStorageUuid()));
            return;
        }

        srcHostUuids.retainAll(dstHostUuids);
        if (srcHostUuids.isEmpty()) {
            completion.fail(operr("can not find hosts both connect to primary storage[uuid: %s] and " +
                    "primary storage[uuid: %s]", msg.getPrimaryStorageUuid(), msg.getTargetPrimaryStorageUuid()));
            return;
        }

        if (!msg.isFull()) {
            throw new OperationFailureException(operr("only support full"));
        }

        SharedBlockKvmCommands.CreateTemplateFromVolumeCmd cmd = new SharedBlockKvmCommands.CreateTemplateFromVolumeCmd();
        cmd.vgUuid = msg.getPrimaryStorageUuid();
        cmd.hostUuid = srcHostUuids.iterator().next();
        cmd.installPath = msg.getInstallPath();
        cmd.volumePath = msg.getVolumeInstallPath();
        cmd.compareQcow2 = msg.isCompareQocw2();
        cmd.provisioning = getPrimaryStorageProvisioningStrategy(msg.getTargetPrimaryStorageUuid());
        new Do(cmd.hostUuid).go(SharedBlockKvmCommands.CREATE_TEMPLATE_FROM_VOLUME_PATH, cmd, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(completion) {
            @Override
            public void success(SharedBlockKvmCommands.AgentRsp returnValue) {
                TakeSnapshotOnSharedBlockGroupPrimaryStorageReply reply = new TakeSnapshotOnSharedBlockGroupPrimaryStorageReply();
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public static String getPrimaryStorageProvisioningStrategy(String primaryStorageUuid) {
        String provisioning = VolumeSystemTags.PRIMARY_STORAGE_VOLUME_PROVISIONING_STRATEGY.getTokenByResourceUuid(
                primaryStorageUuid, PrimaryStorageVO.class, VolumeSystemTags.PRIMARY_STORAGE_VOLUME_PROVISIONING_STRATEGY_TOKEN);
        return provisioning == null ? VolumeProvisioningStrategy.ThickProvisioning.toString() : provisioning;
    }

    @Override
    public void handle(MigrateVolumesBetweenSharedBlockGroupPrimaryStorageMsg msg, ReturnValueCompletion<MigrateVolumesBetweenSharedBlockGroupPrimaryStorageReply> completion) {
        Set<String> srcHostUuids = primaryStorageFactory.getConnectedHostsForOperation(msg.getPrimaryStorageUuid())
                .stream().map(HostInventory::getUuid).collect(Collectors.toSet());
        if (srcHostUuids.isEmpty()) {
            completion.fail(operr("can not find any available host to migrate volume[uuid: %s] between " +
                    "shared block group primary storage[uuid: %s] and [uuid: %s]", msg.getMigrateVolumeStructs().get(0).volumeUuid,
                    msg.getPrimaryStorageUuid(), msg.getTargetPrimaryStorageUuid()));
            return;
        }

        Set<String> dstHostUuids = primaryStorageFactory.getConnectedHostsForOperation(msg.getTargetPrimaryStorageUuid())
                .stream().map(HostInventory::getUuid).collect(Collectors.toSet());
        if (dstHostUuids.isEmpty()) {
            completion.fail(operr("can not find any available host to migrate for volume[uuid: %s] on " +
                    "shared block group primary storage[uuid: %s] and [uuid: %s]", msg.getMigrateVolumeStructs().get(0).volumeUuid,
                    msg.getTargetPrimaryStorageUuid(), msg.getTargetPrimaryStorageUuid()));
            return;
        }

        srcHostUuids.retainAll(dstHostUuids);
        if (srcHostUuids.isEmpty()) {
            completion.fail(operr("can not find hosts both connect to primary storage[uuid: %s] and " +
                    "primary storage[uuid: %s]", msg.getPrimaryStorageUuid(), msg.getTargetPrimaryStorageUuid()));
            return;
        }

        SharedBlockKvmCommands.MigrateDataCmd cmd = new SharedBlockKvmCommands.MigrateDataCmd();
        cmd.vgUuid = msg.getPrimaryStorageUuid();
        cmd.hostUuid = srcHostUuids.iterator().next();
        cmd.volumePath = msg.getVolumePath();
        cmd.migrateVolumeStructs = msg.getMigrateVolumeStructs();
        cmd.provisioning = getPrimaryStorageProvisioningStrategy(msg.getTargetPrimaryStorageUuid());
        cmd.getAddons().put(SharedBlockConstants.DEVICE_ALLOCATE_STRATEGY, getPvAllocateStrategy(cmd.vgUuid, msg.getTargetPrimaryStorageUuid()));
        new Do(cmd.hostUuid).go(SharedBlockKvmCommands.MIGRATE_DATA_PATH, cmd, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(completion) {
            @Override
            public void success(SharedBlockKvmCommands.AgentRsp returnValue) {
                MigrateVolumesBetweenSharedBlockGroupPrimaryStorageReply reply = new MigrateVolumesBetweenSharedBlockGroupPrimaryStorageReply();
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void handle(APIRefreshSharedblockDeviceCapacityMsg msg, Completion completion) {
        SharedBlockKvmCommands.CheckDisksCmd cmd = new SharedBlockKvmCommands.CheckDisksCmd();
        if (msg.getUuid() != null) {
            cmd.sharedBlockUuids = Q.New(SharedBlockVO.class)
                    .select(SharedBlockVO_.diskUuid)
                    .in(SharedBlockVO_.uuid, Collections.singletonList(msg.getUuid()))
                    .listValues();
        }
        if (cmd.sharedBlockUuids == null) {
            cmd.sharedBlockUuids = Q.New(SharedBlockVO.class)
                    .select(SharedBlockVO_.diskUuid)
                    .eq(SharedBlockVO_.sharedBlockGroupUuid, msg.getPrimaryStorageUuid())
                    .listValues();
        }
        cmd.vgUuid = msg.getPrimaryStorageUuid();
        cmd.rescan = true;

        List<HostInventory> hinvs = primaryStorageFactory.getConnectedHostsForOperation(getSelfInventory());

        if (hinvs.isEmpty()) {
            completion.fail(operr("cannot find any connected host to perform the operation, it seems all KVM hosts" +
                            " attached with the shared block group storage[uuid:%s] are disconnected",
                    msg.getPrimaryStorageUuid()));
            return;
        }
        logger.debug(String.format("get connected hosts: %s", hinvs.parallelStream()
                .map(HostInventory::getUuid).collect(Collectors.toList())));

        ErrorCodeList errList = new ErrorCodeList();
        new While<>(hinvs).step((hinv, whileCompletion) -> {
            cmd.hostUuid = hinv.getUuid();
            new Do(hinv.getUuid()).go(SharedBlockKvmCommands.CHECK_DISKS_PATH, cmd, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(completion) {
                @Override
                public void success(SharedBlockKvmCommands.AgentRsp returnValue) {
                    whileCompletion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    errList.getCauses().add(errorCode);
                    whileCompletion.done();
                }
            });
        }, 3).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errList.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errList.getCauses().get(0));
                }
            }
        });
    }

    @Override
    void handle(DownloadBitsFromKVMHostToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadBitsFromKVMHostToPrimaryStorageReply> completion) {
        DownloadBitsFromKVMHostToPrimaryStorageReply reply = new DownloadBitsFromKVMHostToPrimaryStorageReply();

        GetKVMHostDownloadCredentialMsg gmsg = new GetKVMHostDownloadCredentialMsg();
        gmsg.setHostUuid(msg.getSrcHostUuid());

        if (PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.hasTag(self.getUuid())) {
            gmsg.setDataNetworkCidr(PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.getTokenByResourceUuid(self.getUuid(), PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY_TOKEN));
        }

        bus.makeTargetServiceIdByResourceUuid(gmsg, HostConstant.SERVICE_ID, msg.getSrcHostUuid());
        bus.send(gmsg, new CloudBusCallBack(reply) {
            @Override
            public void run(MessageReply rly) {
                if (!rly.isSuccess()) {
                    completion.fail(rly.getError());
                    return;
                }

                GetKVMHostDownloadCredentialReply grly = rly.castReply();
                DownloadBitsFromKVMHostCmd cmd = new DownloadBitsFromKVMHostCmd();
                cmd.setHostname(grly.getHostname());
                cmd.setUsername(grly.getUsername());
                cmd.setSshKey(grly.getSshKey());
                cmd.setSshPort(grly.getSshPort());
                cmd.setBackupStorageInstallPath(msg.getHostInstallPath());
                cmd.setPrimaryStorageInstallPath(msg.getPrimaryStorageInstallPath());
                cmd.setBandWidth(msg.getBandWidth());
                cmd.setIdentificationCode(msg.getLongJobUuid() + msg.getPrimaryStorageInstallPath());
                new Do(msg.getDestHostUuid()).go(DOWNLOAD_BITS_FROM_KVM_HOST_PATH, cmd, DownloadBitsFromKVMHostRsp.class, new ReturnValueCompletion<DownloadBitsFromKVMHostRsp>(completion) {
                    @Override
                    public void success(DownloadBitsFromKVMHostRsp rsp) {
                        reply.setFormat(rsp.format);
                        completion.success(reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }
        });
    }

    @Override
    void handle(CancelDownloadBitsFromKVMHostToPrimaryStorageMsg msg, ReturnValueCompletion<CancelDownloadBitsFromKVMHostToPrimaryStorageReply> completion) {
        CancelDownloadBitsFromKVMHostToPrimaryStorageReply reply = new CancelDownloadBitsFromKVMHostToPrimaryStorageReply();
        CancelDownloadBitsFromKVMHostCmd cmd = new CancelDownloadBitsFromKVMHostCmd();
        cmd.setPrimaryStorageInstallPath(msg.getPrimaryStorageInstallPath());
        new Do(msg.getDestHostUuid()).go(SharedBlockKvmCommands.CANCEL_DOWNLOAD_BITS_FROM_KVM_HOST_PATH, cmd, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(completion) {
            @Override
            public void success(SharedBlockKvmCommands.AgentRsp returnValue) {
                if (returnValue.success) {
                    completion.success(reply);
                } else {
                    completion.fail(operr("operation error, because:%s", returnValue.error));
                }
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(GetDownloadBitsFromKVMHostProgressMsg msg, ReturnValueCompletion<GetDownloadBitsFromKVMHostProgressReply> completion) {
        GetDownloadBitsFromKVMHostProgressReply reply = new GetDownloadBitsFromKVMHostProgressReply();
        GetDownloadBitsFromKVMHostProgressCmd cmd = new GetDownloadBitsFromKVMHostProgressCmd();
        cmd.volumePaths = msg.getVolumePaths();
        new Do(msg.getHostUuid()).go(GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH, cmd, GetDownloadBitsFromKVMHostProgressRsp.class, new ReturnValueCompletion<GetDownloadBitsFromKVMHostProgressRsp>(completion) {
            @Override
            public void success(GetDownloadBitsFromKVMHostProgressRsp rsp) {
                reply.setTotalSize(rsp.totalSize);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(GetVolumeBackingChainFromPrimaryStorageMsg msg, ReturnValueCompletion<GetVolumeBackingChainFromPrimaryStorageReply> completion) {
        String hostUuid = primaryStorageFactory.getConnectedHostsForOperationToleranceDeleted(getSelfInventory(), msg.getVolumeUuid())
                .get(0).getUuid();

        GetVolumeBackingChainFromPrimaryStorageReply reply = new GetVolumeBackingChainFromPrimaryStorageReply();
        ErrorCodeList err = new ErrorCodeList();
        new While<>(msg.getRootInstallPaths()).each((installPath, compl) -> {
            GetBackingChainCmd cmd = new GetBackingChainCmd();
            cmd.volumeUuid = msg.getVolumeUuid();
            cmd.installPath = installPath;
            cmd.containSelf = false;
            cmd.vgUuid = msg.getPrimaryStorageUuid();
            cmd.hostUuid = hostUuid;
            httpCall(GET_BACKING_CHAIN_PATH, hostUuid, cmd, SharedBlockKvmCommands.GetBackingChainRsp.class, new ReturnValueCompletion<GetBackingChainRsp>(compl) {
                @Override
                public void success(GetBackingChainRsp rsp) {
                    if (rsp.backingChain == null || rsp.backingChain.isEmpty()) {
                        reply.putBackingChainInstallPath(installPath, Collections.emptyList());
                        reply.putBackingChainSize(installPath, 0L);
                    } else {
                        reply.putBackingChainInstallPath(installPath, rsp.backingChain.stream()
                                .map(it -> makeSblkInstallUrl(it)).collect(Collectors.toList()));
                        reply.putBackingChainSize(installPath, rsp.totalSize);
                    }

                    compl.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    err.getCauses().add(errorCode);
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!err.getCauses().isEmpty()) {
                    completion.fail(err.getCauses().get(0));
                } else {
                    completion.success(reply);
                }
            }
        });
    }

    @Override
    void handle(ConfigureFilterMsg msg, Completion completion) {
        doConfigFilter(Collections.singletonList(msg.getClusterUuid()), msg.getPrimaryStorageUuid(), new ArrayList<>(), completion);
    }

    void doConfigFilter(List<String> clusterUuids, String psUuid, List<String> exceptSharedBlockUuids, Completion completion) {
        List<String> hostUuids = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .in(HostVO_.clusterUuid, clusterUuids)
                .eq(HostVO_.status, HostStatus.Connected)
                .listValues();
        if (hostUuids.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(hostUuids).each((hostUuid, whileCompletion) -> {
            List<String> sharedblockUuids = getAllSharedBlockUuidsOnHost(hostUuid);
            if (exceptSharedBlockUuids != null && !exceptSharedBlockUuids.isEmpty()) {
                sharedblockUuids = sharedblockUuids.stream().filter(u -> !exceptSharedBlockUuids.contains(u)).collect(Collectors.toList());
            }

            ConfigFilterCmd cmd = new ConfigFilterCmd();
            cmd.allSharedBlockUuids = sharedblockUuids;
            cmd.vgUuid = psUuid;
            cmd.hostUuid = hostUuid;
            httpCall(CONFIG_FILTER_PATH, hostUuid, cmd, SharedBlockKvmCommands.AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
                @Override
                public void success(AgentRsp rsp) {
                    whileCompletion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    private void shrinkVolumeSnapShot(VolumeSnapshotVO snapshotVO, VolumeVO volumeVO, ReturnValueCompletion<ShrinkVolumeSnapshotOnPrimaryStorageReply> completion) {
        ShrinkVolumeSnapshotOnPrimaryStorageReply sreply = new ShrinkVolumeSnapshotOnPrimaryStorageReply();

        if (!volumeVO.getStatus().equals(VolumeStatus.Ready)) {
            throw new OperationFailureException(operr("cannot shrink snapshot %s, because volume %s not ready",
                    snapshotVO.getUuid(), volumeVO.getUuid()));
        }

        ShrinkResult result = new ShrinkResult();
        result.setOldSize(snapshotVO.getSize());
        result.setSize(snapshotVO.getSize());
        result.setDeltaSize(0);
        sreply.setShrinkResult(result);

        if (snapshotVO.isFullSnapshot() || volumeVO.isShareable()) {
            completion.success(sreply);
            return;
        }

        String provision = getVolumeProvision(volumeVO.getUuid(), volumeVO.getInstallPath());
        if (VolumeProvisioningStrategy.ThinProvisioning.toString().equals(provision)) {
            completion.success(sreply);
            return;
        }

        String hostUuid = null;
        if (volumeVO.isAttached() && volumeVO.getVmInstanceUuid() != null) {
            VmInstanceVO instanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, volumeVO.getVmInstanceUuid()).find();
            if (VmInstanceState.Running != instanceVO.getState() && VmInstanceState.Stopped != instanceVO.getState()) {
                throw new OperationFailureException(operr("cannot shrink snapshot %s, beacuse vm %s not in Running/Stopped state", snapshotVO.getUuid(), instanceVO.getUuid()));
            }

            if (instanceVO.getHostUuid() != null) {
                hostUuid = instanceVO.getHostUuid();
            }
        }

        List<String> hosts = primaryStorageFactory
                .getConnectedHostsForOperation(volumeVO.getPrimaryStorageUuid()).stream()
                .map(HostInventory::getUuid)
                .collect(Collectors.toList());

        final String finalHost = hostUuid != null ? hostUuid : hosts.get(0);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("shrink-volume-snapshot-%s-on-sharedblock", snapshotVO.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = String.format("deactivate-snapshot-%s-on-other-hosts", snapshotVO.getUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ErrorCodeList errList = new ErrorCodeList();
                hosts.remove(finalHost);
                if (hosts.isEmpty()) {
                    trigger.next();
                    return;
                }

                new While<>(hosts).step((hUuid, completion1) -> {
                    bulkActiveVolumes(Collections.singletonList(snapshotVO.getPrimaryStorageInstallPath()), snapshotVO.getPrimaryStorageUuid(), hUuid,
                            LvmlockdLockingType.NULL, true, false, new Completion(completion1) {
                                @Override
                                public void success() {
                                    completion1.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    errList.getCauses().add(errorCode);
                                    completion1.done();
                                }
                            });
                }, 5).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errList.getCauses().isEmpty()) {
                            trigger.fail(errList.getCauses().get(0));
                            return;
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("shrink-volume-snapshot-%s", snapshotVO.getUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ShrinkSnapshotCmd scmd = new ShrinkSnapshotCmd();
                scmd.installPath = snapshotVO.getPrimaryStorageInstallPath();
                scmd.vgUuid = snapshotVO.getPrimaryStorageUuid();
                new Do(finalHost).go(SHRINK_SNAPSHOT_PATH, scmd, ShrinkSnapshotRsp.class, new ReturnValueCompletion<ShrinkSnapshotRsp>(completion) {
                    @Override
                    public void success(ShrinkSnapshotRsp reply) {
                        logger.debug(String.format("successfully shrink sblk snapshot[%s] from %s to %s ", snapshotVO.getUuid(), reply.oldSize, reply.size));

                        result.setOldSize(reply.oldSize);
                        result.setSize(reply.size);
                        result.setDeltaSize(reply.oldSize - reply.size);

                        if (result.getDeltaSize() != 0) {
                            logger.debug(String.format("after shrink sblk snapshot, release snapshot size[%s] on primary storage[%s] for volume[uuid:%s]",
                                    result.getDeltaSize(), volumeVO.getPrimaryStorageUuid(), volumeVO.getUuid()));
                            ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                            rmsg.setPrimaryStorageUuid(volumeVO.getPrimaryStorageUuid());
                            rmsg.setDiskSize(result.getDeltaSize());
                            rmsg.setNoOverProvisioning(true);
                            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, volumeVO.getPrimaryStorageUuid());
                            bus.send(rmsg);
                        }
                        completion.success(sreply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(sreply);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    void handle(ShrinkVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<ShrinkVolumeSnapshotOnPrimaryStorageReply> completion) {
        VolumeSnapshotVO snapshotVO = dbf.findByUuid(msg.getSnapshotUuid(), VolumeSnapshotVO.class);
        VolumeVO volumeVO = dbf.findByUuid(snapshotVO.getVolumeUuid(), VolumeVO.class);

        thdf.chainSubmit(new ChainTask(completion) {

            @Override
            public String getSyncSignature() {
                return String.format("volume-sync-signature-%s", volumeVO.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                shrinkVolumeSnapShot(snapshotVO, volumeVO, new ReturnValueCompletion<ShrinkVolumeSnapshotOnPrimaryStorageReply>(chain) {
                    @Override
                    public void success(ShrinkVolumeSnapshotOnPrimaryStorageReply returnValue) {
                        completion.success(returnValue);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    void handle(ActivateVolumeOnPrimaryStorageMsg msg, Completion completion) {
        activeVolumeByInstallPath(msg.getInstallPath(), msg.getPrimaryStorageUuid(), msg.getHostUuid(),
                LvmlockdLockingType.valueOf(msg.getLockingType()), msg.isRecursive(), new Completion(completion) {
                    @Override
                    public void success() {
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    @Override
    void handle(GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg, ReturnValueCompletion<GetVolumeSnapshotEncryptedOnPrimaryStorageReply> completion) {
        VolumeSnapshotVO snapshotVO = dbf.findByUuid(msg.getSnapshotUuid(), VolumeSnapshotVO.class);
        VolumeVO volumeVO = dbf.findByUuid(snapshotVO.getVolumeUuid(), VolumeVO.class);

        String hostUuid = null;
        if (volumeVO.isAttached() && volumeVO.getVmInstanceUuid() != null) {
            VmInstanceVO instanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, volumeVO.getVmInstanceUuid()).find();
            if (VmInstanceState.Running != instanceVO.getState() && VmInstanceState.Stopped != instanceVO.getState()) {
                throw new OperationFailureException(operr("cannot shrink snapshot %s, beacuse vm %s not in Running/Stopped state", snapshotVO.getUuid(), instanceVO.getUuid()));
            }

            if (instanceVO.getHostUuid() != null) {
                hostUuid = instanceVO.getHostUuid();
            }
        }

        List<String> hosts = primaryStorageFactory
                .getConnectedHostsForOperation(volumeVO.getPrimaryStorageUuid()).stream()
                .map(HostInventory::getUuid)
                .collect(Collectors.toList());
        final String finalHost = hostUuid != null ? hostUuid : hosts.get(0);

        GetQcow2HashValueCmd cmd = new GetQcow2HashValueCmd();
        cmd.setInstallPath(msg.getPrimaryStorageInstallPath());

        new Do(finalHost).go(GET_QCOW2_HASH_VALUE_PATH, cmd, GetQcow2HashValueRsp.class, new ReturnValueCompletion<GetQcow2HashValueRsp>(completion) {
            @Override
            public void success(GetQcow2HashValueRsp rsp) {
                GetVolumeSnapshotEncryptedOnPrimaryStorageReply reply = new GetVolumeSnapshotEncryptedOnPrimaryStorageReply();
                reply.setSnapshotUuid(msg.getSnapshotUuid());
                reply.setEncrypt(rsp.getHashValue());
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void ping(String psUuid, Completion completion) {
        AgentCmd cmd = new AgentCmd();
        cmd.vgUuid = psUuid;
        new KvmAgentCommandDispatcher(psUuid).go(PING_PATH, cmd, SharedBlockKvmCommands.AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("ping sblk[%s] failed but return success, details: %s", psUuid, errorCode.getDetails()));
                completion.success();
            }
        });
    }

    static void updateCapacity(SharedBlockKvmCommands.AgentRsp rsp) {
        DatabaseFacade dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);

        if (rsp.lunCapacities == null) {
            return;
        }
        List<SharedBlockCapacityVO> addVos = new ArrayList<>(), updateVos = new ArrayList<>();
        rsp.lunCapacities.forEach(lunCapacity -> {
            String wwid = lunCapacity.getWwid();
            long totalCapacity = lunCapacity.getTotalCapacity();
            long availableCapacity = lunCapacity.getAvailableCapacity();
            String SharedBlockUuid = Q.New(SharedBlockVO.class).select(SharedBlockVO_.uuid).eq(SharedBlockVO_.diskUuid, wwid).findValue();
            SharedBlockCapacityVO vo = dbf.findByUuid(SharedBlockUuid, SharedBlockCapacityVO.class);
            if (vo == null) {
                vo = new SharedBlockCapacityVO();
                vo.setUuid(SharedBlockUuid);
                vo.setTotalCapacity(totalCapacity);
                vo.setAvailableCapacity(availableCapacity);
                addVos.add(vo);
                return;
            }
            vo.setTotalCapacity(totalCapacity);
            vo.setAvailableCapacity(availableCapacity);
            updateVos.add(vo);
        });
        if (!addVos.isEmpty()) {
            dbf.persistCollection(addVos);
        }
        if (!updateVos.isEmpty()) {
            dbf.updateCollection(updateVos);
        }
    }

    @Override
    public void beforeTakeLiveSnapshotsOnVolumes(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmMsg tmsg, Map flowData, Completion completion) {
        class VmVolumesStruct {
            public VmInstanceVO vmInstanceVO;
            public List<CreateVolumesSnapshotsJobStruct> jobs;
            public final List<String> createdLvInstallPaths = Collections.synchronizedList(new ArrayList<>());
        }

        List<CreateVolumesSnapshotsJobStruct> sharedBlockVolumeSnapshotJobs = new ArrayList<>();

        for (CreateVolumesSnapshotsJobStruct struct : msg.getVolumeSnapshotJobs()) {
            if (isSharedBlockGroupPrimaryStorage(struct.getPrimaryStorageUuid())) {
                sharedBlockVolumeSnapshotJobs.add(struct);
            }
        }

        if (sharedBlockVolumeSnapshotJobs.isEmpty()) {
            completion.success();
            return;
        }

        if (msg.getLockedVmInstanceUuids() == null || msg.getLockedVmInstanceUuids().isEmpty()) {
            // no need to pre create lv if no vm attached
            completion.success();
            return;
        }

        List<VmVolumesStruct> vmVolumesStructs = Collections.synchronizedList(new ArrayList<>());
        for (String vmUuid : msg.getLockedVmInstanceUuids()) {
            VmVolumesStruct vmVolumesStruct = new VmVolumesStruct();
            vmVolumesStruct.vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).find();
            List<String> vmVolumesUuids = vmVolumesStruct.vmInstanceVO.getAllDiskVolumes()
                    .stream().map(VolumeVO::getUuid).collect(Collectors.toList());
            vmVolumesStruct.jobs = sharedBlockVolumeSnapshotJobs.stream()
                    .filter(s -> vmVolumesUuids.contains(s.getVolumeUuid())
                            || (vmVolumesStruct.vmInstanceVO.getMemoryVolume() != null
                            && s.getVolumeUuid().equals(vmVolumesStruct.vmInstanceVO.getMemoryVolume().getUuid())))
                    .collect(Collectors.toList());
            vmVolumesStructs.add(vmVolumesStruct);
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("create lvs for take live snapshots on volumes");
        chain.then(new Flow() {
            String __name__ = "create snapshot lvs";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ErrorCodeList errList = new ErrorCodeList();
                new While<>(vmVolumesStructs).step((vmVolumesStruct, whileCompletion) -> {
                    if (vmVolumesStruct.jobs == null || vmVolumesStruct.jobs.isEmpty()) {
                        whileCompletion.done();
                        return;
                    }

                    if (vmVolumesStruct.vmInstanceVO.getState().equals(VmInstanceState.Stopped)) {
                        whileCompletion.done();
                        return;
                    }

                    ErrorCodeList errList1 = new ErrorCodeList();
                    new While<>(vmVolumesStruct.jobs).step((job, whileCompletion1) -> {
                        String installPath = makeInstallPath(job.getPrimaryStorageUuid(), job.getVolumeSnapshotStruct().getCurrent().getUuid());
                        Long volumeSize;
                        if (isMemoryVolume(job.getVolumeUuid())) {
                            String vmInstanceUuid = Q.New(VolumeVO.class).select(VolumeVO_.vmInstanceUuid)
                                    .eq(VolumeVO_.uuid, job.getVolumeUuid())
                                    .findValue();
                            volumeSize = Q.New(VmInstanceVO.class)
                                    .select(VmInstanceVO_.memorySize)
                                    .eq(VmInstanceVO_.uuid, vmInstanceUuid)
                                    .findValue();
                        } else {
                            volumeSize = Q.New(VolumeVO.class).select(VolumeVO_.size).eq(VolumeVO_.uuid, job.getVolumeUuid()).findValue();
                        }

                        createEmptyVolumeByInstallPath(installPath, volumeSize,
                                job.getVolumeSnapshotStruct().getCurrent().getUuid(),
                                vmVolumesStruct.vmInstanceVO.getHostUuid(), new Completion(whileCompletion1) {
                            @Override
                            public void success() {
                                vmVolumesStruct.createdLvInstallPaths.add(installPath);
                                whileCompletion1.done();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                errList1.getCauses().add(errorCode);
                                whileCompletion1.done();
                            }
                        });
                    }, 5).run(new WhileDoneCompletion(whileCompletion) {
                        @Override
                        public void done(ErrorCodeList errorCodeList) {
                            if (!errList1.getCauses().isEmpty()) {
                                errList.getCauses().add(errList1.getCauses().get(0));
                            }
                            whileCompletion.done();
                        }
                    });
                }, 5).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errList.getCauses().isEmpty()) {
                            trigger.next();
                        } else {
                            trigger.fail(errList.getCauses().get(0));
                        }
                    }
                });
            }

            private boolean isMemoryVolume(String volumeUuid) {
                return Q.New(VolumeVO.class)
                        .eq(VolumeVO_.uuid, volumeUuid)
                        .eq(VolumeVO_.type, VolumeType.Memory)
                        .isExists();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                List<String> installPaths = new ArrayList<>();
                for (VmVolumesStruct s : vmVolumesStructs) {
                    installPaths.addAll(s.createdLvInstallPaths);
                }

                if (installPaths.isEmpty()) {
                    trigger.rollback();
                    return;
                }

                new While<>(installPaths).step((installPath, whileCompletion) -> {
                    deleteBits(installPath, new Completion(trigger) {
                        @Override
                        public void success() {
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            whileCompletion.done();
                        }
                    });
                }, 5).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.rollback();
                    }
                });
            }
        }).then(new Flow() {
            String __name__ = "active snapshot lvs";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<Pair<String, VmInstanceVO>> pathVmPairs = new ArrayList<>();
                for (VmVolumesStruct struct : vmVolumesStructs) {
                    for (String path : struct.createdLvInstallPaths) {
                        pathVmPairs.add(new Pair<>(path, struct.vmInstanceVO));
                    }
                }

                new While<>(pathVmPairs).step((pair, whileCompletion) -> {
                    String installPath = pair.first();
                    VmInstanceVO vm = pair.second();
                    if (installPath == null) {
                        whileCompletion.addError(operr("get null install path in snapshot for vm %s", vm.getUuid()));
                        whileCompletion.done();
                        return;
                    }

                    activeVolumeByInstallPath(installPath,
                            getPrimaryStorageUuidByInstallPath(installPath),
                            vm.getHostUuid(),
                            LvmlockdLockingType.EXCLUSIVE, false, new Completion(whileCompletion) {
                                @Override
                                public void success() {
                                    whileCompletion.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    whileCompletion.addError(errorCode);
                                    whileCompletion.done();
                                }
                            });

                }, 5).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.hasError()) {
                            trigger.fail(multiErr(errorCodeList));
                            return;
                        }
                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                for (TakeSnapshotsOnKvmJobStruct struct : tmsg.getSnapshotJobs()) {
                    if(struct.getInstallPath() == null) {
                        continue;
                    }
                    if (struct.getInstallPath().startsWith(SHARED_BLOCK_INSTALL_PATH_SCHEME)) {
                        struct.setInstallPath(convertInstallPathToAbsolute(struct.getInstallPath()));
                    }

                    if (struct.getNewVolumeInstallPath() == null) {
                        continue;
                    }
                    if (struct.getNewVolumeInstallPath().startsWith(SHARED_BLOCK_INSTALL_PATH_SCHEME)) {
                        struct.setNewVolumeInstallPath(convertInstallPathToAbsolute(struct.getInstallPath()));
                    }
                }
                completion.success();
            }
        }).start();
    }

    private String makeSblkInstallUrl(String path) {
        return path.replace("/dev/", SHARED_BLOCK_INSTALL_PATH_SCHEME);
    }

    @Override
    public String getPrimaryStorageType() {
        return SHARED_BLOCK_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public void protect(VolumeSnapshotInventory snapshot, Completion completion) {
        completion.success();
        //NOTE(weiw): skip sharedblock protector
//        VolumeSnapshotTreeVO treeVO = Q.New(VolumeSnapshotTreeVO.class)
//                .eq(VolumeSnapshotTreeVO_.uuid, snapshot.getTreeUuid())
//                .find();
//
//        if (!treeVO.isCurrent()) {
//            completion.success();
//            return;
//        }
//
//        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, snapshot.getVolumeUuid()).find();
//
//        GetBackingChainCmd cmd = new GetBackingChainCmd();
//        cmd.volumeUuid = snapshot.getVolumeUuid();
//        cmd.installPath = volumeVO.getInstallPath();
//        cmd.vgUuid = snapshot.getPrimaryStorageUuid();
//
//        String hostUuid = Q.New(VmInstanceVO.class).select(VmInstanceVO_.hostUuid)
//                .eq(VmInstanceVO_.uuid, volumeVO.getVmInstanceUuid()).findValue();
//        cmd.hostUuid = hostUuid;
//        if (cmd.hostUuid == null) {
//            List<HostInventory> hosts = primaryStorageFactory.getConnectedHostsForOperation(snapshot.getPrimaryStorageUuid());
//            if (hosts.isEmpty()) {
//                completion.success();
//                return;
//            }
//
//            cmd.hostUuid = hosts.get(0).getUuid();
//        }
//        httpCall(GET_BACKING_CHAIN_PATH, cmd.hostUuid, cmd, SharedBlockKvmCommands.GetBackingChainRsp.class, new ReturnValueCompletion<GetBackingChainRsp>(completion) {
//            @Override
//            public void success(GetBackingChainRsp rsp) {
//                if (rsp.backingChain == null || rsp.backingChain.isEmpty()) {
//                    completion.fail(inerr("snapshot[installPath:%s] is in current snapshot tree " +
//                            "but volume backing chain is empty", snapshot.getPrimaryStorageInstallPath()));
//                }
//
//                String absPath = convertInstallPathToAbsolute(snapshot.getPrimaryStorageInstallPath());
//                if (rsp.backingChain.contains(absPath)) {
//                    completion.success();
//                } else {
//                    completion.fail(inerr("snapshot[installPath:%s] is in current snapshot tree " +
//                            "but not in volume backing chain[%s]", snapshot.getPrimaryStorageInstallPath(), rsp.backingChain));
//                }
//            }
//
//            @Override
//            public void fail(ErrorCode errorCode) {
//                completion.fail(errorCode);
//            }
//        });
    }

    static class BlockCommitStruct {
        boolean online;
        String hostUuid;
        VolumeInventory volume;
        VolumeSnapshotInventory srcSnapshot;
        VolumeSnapshotInventory dstSnapshot;
        List<String> srcChildrenInstallPathInDb;

        BlockCommitStruct(boolean online, String hostUuid, VolumeInventory volume, VolumeSnapshotInventory srcSnapshot, VolumeSnapshotInventory dstSnapshot, List<String> srcChildrenInstallPathInDb) {
            this.online = online;
            this.hostUuid = hostUuid;
            this.volume = volume;
            this.srcSnapshot = srcSnapshot;
            this.dstSnapshot = dstSnapshot;
            this.srcChildrenInstallPathInDb = srcChildrenInstallPathInDb;
        }
    }

    @Override
    public void beforeBlockCommit(KVMHostInventory host, CommitVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockCommitCmd cmd, Completion completion) {
        if (!isSharedBlockGroupPrimaryStorage(msg.getVolume().getPrimaryStorageUuid())) {
            completion.success();
            return;
        }

        BlockCommitStruct struct = new BlockCommitStruct(true, host.getUuid(), msg.getVolume(),
                msg.getSrcSnapshot(), msg.getDstSnapshot(), msg.getSrcChildrenInstallPathInDb());
        doBeforeBlockCommit(struct, completion);
    }

    private void doBeforeBlockCommit(BlockCommitStruct struct, Completion completion) {
        VolumeInventory volume = struct.volume;
        String hostUuid = struct.hostUuid;
        String dstPath = struct.dstSnapshot.getPrimaryStorageInstallPath();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("before-block-commit-volume");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("deactivate-dstPath-%s-on-hosts", dstPath);

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ErrorCodeList errList = new ErrorCodeList();
                        List<HostInventory> hosts = sharedBlockGroupFactory.getConnectedHostsForOperation(volume.getPrimaryStorageUuid());
                        hosts.removeIf(h -> h.getUuid().equals(hostUuid));
                        new While<>(hosts).step((hostInv, completion1) -> {
                            activeVolumeByInstallPath(convertInstallPathToAbsolute(dstPath), volume.getPrimaryStorageUuid(), hostInv.getUuid(),
                                    LvmlockdLockingType.NULL, false, new Completion(completion) {
                                        @Override
                                        public void success() {
                                            completion1.done();
                                        }

                                        @Override
                                        public void fail(ErrorCode errorCode) {
                                            errList.getCauses().add(errorCode);
                                            completion1.done();
                                        }
                                    });
                        }, 5).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errList.getCauses().isEmpty()) {
                                    trigger.fail(operr("deactive installPath failed, " +
                                            "because %s", errorCodeList.getCauses().toString()));
                                    return;
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = String.format("extend-dstPath-%s", dstPath);

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ExtendLogicalVolumeCmd cmd = new ExtendLogicalVolumeCmd();
                        cmd.destPath = convertInstallPathToAbsolute(struct.dstSnapshot.getPrimaryStorageInstallPath());
                        cmd.volumeUuid = struct.volume.getUuid();
                        cmd.vgUuid = struct.volume.getPrimaryStorageUuid();

                        String provision = getVolumeProvision(struct.volume.getUuid(), struct.volume.getInstallPath());
                        if (provision != null && provision.equals(VolumeProvisioningStrategy.ThinProvisioning.toString())) {
                            cmd.requiredSize = Math.min(struct.srcSnapshot.getSize() + struct.dstSnapshot.getSize(), struct.volume.getSize());
                        } else {
                            cmd.requiredSize = struct.volume.getSize();
                        }
                        httpCall(EXTEND_LOGICAL_VOLUME_PATH, struct.hostUuid, cmd, ExtendLogicalVolumeRsp.class, new ReturnValueCompletion<ExtendLogicalVolumeRsp>(trigger) {
                            @Override
                            public void success(ExtendLogicalVolumeRsp rsp) {
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
                    String __name__ = String.format("active-srcPath-%s-children-path", struct.srcSnapshot.getPrimaryStorageInstallPath());

                    @Override
                    public boolean skip(Map data) {
                        return !struct.online;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(struct.srcChildrenInstallPathInDb).step((childrenPath, whileCompletion) -> {
                            if (Objects.equals(childrenPath, volume.getInstallPath())) {
                                whileCompletion.done();
                                return;
                            }

                            activeVolumeByInstallPath(convertInstallPathToAbsolute(childrenPath), volume.getPrimaryStorageUuid(), hostUuid,
                                    LvmlockdLockingType.SHARE, false, new Completion(completion) {
                                        @Override
                                        public void success() {
                                            whileCompletion.done();
                                        }

                                        @Override
                                        public void fail(ErrorCode errorCode) {
                                            whileCompletion.addError(operr("active installPath %s failed, " +
                                                    "because %s", childrenPath, errorCode.getDetails()));
                                            whileCompletion.done();
                                        }
                                    });
                        }, 5).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    trigger.fail(operr("active children snapshot failed, because %s",
                                            errorCodeList.getCauses().toString()));
                                    return;
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void afterBlockCommit(KVMHostInventory host, CommitVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockCommitCmd cmd, CommitVolumeSnapshotOnHypervisorReply reply, Completion completion) {
        if (!isSharedBlockGroupPrimaryStorage(msg.getVolume().getPrimaryStorageUuid())) {
            completion.success();
            return;
        }

        String hostUuid = host.getUuid();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("after-before-block-commit-volume");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__= "activate-new-volume-path";

                    @Override
                    public boolean skip(Map data) {
                        return !cmd.getTop().equals(cmd.getVolume().getInstallPath());
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        activeVolumeByInstallPath(msg.getDstSnapshot().getPrimaryStorageInstallPath(), msg.getVolume().getPrimaryStorageUuid(), msg.getHostUuid(), LvmlockdLockingType.EXCLUSIVE, false, new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    logger.warn(String.format("failed to activate new volume installpath %s: %s", msg.getDstSnapshot().getPrimaryStorageInstallPath(), errorCode.getDetails()));
                                    trigger.next();
                                }
                            });
                    }
                });
                flow(new NoRollbackFlow() {
                    String __name__ = "deactivate-children-installPath";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(msg.getSrcChildrenInstallPathInDb()).each((childrenPath, whileCompletion) -> {
                            if (Objects.equals(childrenPath, msg.getVolume().getInstallPath())) {
                                whileCompletion.done();
                                return;
                            }

                            activeVolumeByInstallPath(convertInstallPathToAbsolute(childrenPath), msg.getVolume().getPrimaryStorageUuid(), hostUuid,
                                    LvmlockdLockingType.NULL, false, new Completion(completion) {
                                        @Override
                                        public void success() {
                                            whileCompletion.done();
                                        }

                                        @Override
                                        public void fail(ErrorCode errorCode) {
                                            whileCompletion.addError(operr("active installPath %s failed, because %s",
                                                    childrenPath, errorCode.getDetails()));
                                            whileCompletion.done();
                                        }
                                    });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    logger.warn(String.format("deactivate children snapshot failed, " +
                                            "because %s", errorCodeList.getCauses().toString()));
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.success();
                    }
                });
            }
        }).start();
    }

    @Override
    public void afterBlockCommitFailed(KVMHostInventory host, CommitVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockCommitCmd cmd, KVMAgentCommands.BlockCommitResponse rsp, ErrorCode err) {
    }

    @Override
    public void beforeBlockPull(KVMHostInventory host, PullVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockPullCmd cmd, Completion completion) {
        if (!isSharedBlockGroupPrimaryStorage(msg.getVolume().getPrimaryStorageUuid())) {
            completion.success();
            return;
        }

        long newSize = Math.min(msg.getDstSnapshot().getSize() + msg.getSrcSnapshot().getSize(), msg.getVolume().getSize());
        String provision = getVolumeProvision(msg.getVolume().getUuid(), msg.getVolume().getInstallPath());
        if (provision == null) {
            logger.debug("dstPath is volume and volume is ThickProvisioning, no need to extend");
            completion.success();
            return;
        }

        ExtendLogicalVolumeCmd extendCmd = new ExtendLogicalVolumeCmd();
        extendCmd.destPath = convertInstallPathToAbsolute(msg.getDstSnapshot().getPrimaryStorageInstallPath());
        extendCmd.volumeUuid = msg.getVolume().getUuid();
        extendCmd.requiredSize = newSize;
        extendCmd.vgUuid = msg.getVolume().getPrimaryStorageUuid();

        new Do(host.getUuid(), msg.getVolume().getPrimaryStorageUuid()).go(EXTEND_LOGICAL_VOLUME_PATH, extendCmd, ExtendLogicalVolumeRsp.class, new ReturnValueCompletion<ExtendLogicalVolumeRsp>(completion) {
            @Override
            public void success(ExtendLogicalVolumeRsp rsp) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void afterBlockPull(KVMHostInventory host, PullVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockPullCmd cmd, PullVolumeSnapshotOnHypervisorReply reply, Completion completion) {
        completion.success();
    }

    @Override
    public void afterBlockPullFailed(KVMHostInventory host, PullVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockPullCmd cmd, KVMAgentCommands.BlockPullResponse rsp, ErrorCode err) {

    }

    @Override
    void handle(CommitVolumeSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<CommitVolumeSnapshotOnPrimaryStorageReply> completion) {
        VolumeInventory volume = msg.getVolume();

        String hostUuid = primaryStorageFactory.getConnectedHostsForOperation(getSelfInventory(), volume).get(0).getUuid();

        CommitVolumeSnapshotOnPrimaryStorageReply reply = new CommitVolumeSnapshotOnPrimaryStorageReply();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("delete-volume-snapshot-by-block-commit");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "before-block-commit";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        BlockCommitStruct struct = new BlockCommitStruct(false, hostUuid, msg.getVolume(),
                                msg.getSrcSnapshot(), msg.getDstSnapshot(), msg.getSrcChildrenInstallPathInDb());
                        doBeforeBlockCommit(struct, new Completion(trigger) {
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
                    String __name__ = "offline-block-commit";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SharedBlockKvmCommands.OfflineCommitSnapshotCmd cmd = new SharedBlockKvmCommands.OfflineCommitSnapshotCmd();
                        cmd.top = convertInstallPathToAbsolute(msg.getSrcSnapshot().getPrimaryStorageInstallPath());
                        cmd.base = convertInstallPathToAbsolute(msg.getDstSnapshot().getPrimaryStorageInstallPath());
                        cmd.topChildrenInstallPathInDb = convertInstallPathsToAbsolute(msg.getSrcChildrenInstallPathInDb());
                        cmd.vgUuid = volume.getPrimaryStorageUuid();
                        cmd.volumeUuid = volume.getUuid();
                        new Do(volume).go(SharedBlockKvmCommands.OFFLINE_COMMIT_SNAPSHOT_PATH, cmd, OfflineCommitSnapshotRsp.class, new ReturnValueCompletion<OfflineCommitSnapshotRsp>(completion) {
                            @Override
                            public void success(OfflineCommitSnapshotRsp returnValue) {
                                reply.setSize(returnValue.getActualSize());
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
                    String __name__ = "deactive-dstSnapshot-installPath-if-need";

                    @Override
                    public boolean skip(Map data) {
                        return !Objects.equals(msg.getSrcSnapshot().getUuid(), volume.getUuid());
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        activeVolumeByInstallPath(convertAbsolutePathToInstall(msg.getDstSnapshot().getPrimaryStorageInstallPath()),
                                msg.getVolume().getPrimaryStorageUuid(), hostUuid, LvmlockdLockingType.NULL, false, new Completion(completion) {
                                    @Override
                                    public void success() {
                                        trigger.next();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        logger.warn(String.format("deactive installPath %s failed, " +
                                                "because %s", msg.getDstSnapshot().getPrimaryStorageInstallPath(), errorCode.getDetails()));
                                        trigger.next();
                                    }
                                });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success(reply);
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    void handle(PullVolumeSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<PullVolumeSnapshotOnPrimaryStorageReply> completion) {
        VolumeInventory volume = msg.getVolume();
        String hostUuid = primaryStorageFactory.getConnectedHostsForOperation(getSelfInventory(), volume).get(0).getUuid();

        PullVolumeSnapshotOnPrimaryStorageReply reply = new PullVolumeSnapshotOnPrimaryStorageReply();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("delete-volume-snapshot-by-block-pull");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("deactivate-dstPath-%s-on-hosts", msg.getDstSnapshot().getPrimaryStorageInstallPath());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ErrorCodeList errList = new ErrorCodeList();
                        List<HostInventory> hosts = sharedBlockGroupFactory.getConnectedHostsForOperation(volume.getPrimaryStorageUuid());
                        hosts.removeIf(h -> h.getUuid().equals(hostUuid));
                        new While<>(hosts).step((hostInv, comp) -> {
                            activeVolumeByInstallPath(convertInstallPathToAbsolute(msg.getDstSnapshot().getPrimaryStorageInstallPath()), msg.getVolume().getPrimaryStorageUuid(), hostInv.getUuid(),
                                    LvmlockdLockingType.NULL, false, new Completion(completion) {
                                        @Override
                                        public void success() {
                                            comp.done();
                                        }

                                        @Override
                                        public void fail(ErrorCode errorCode) {
                                            errList.getCauses().add(errorCode);
                                            comp.done();
                                        }
                                    });
                        }, 5).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errList.getCauses().isEmpty()) {
                                    trigger.fail(operr("deactive installPath failed, " +
                                            "because %s", errorCodeList.getCauses().toString()));
                                    return;
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = String.format("extend-dstPath-%s", msg.getDstSnapshot().getPrimaryStorageInstallPath());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        long newSize = Math.min(msg.getDstSnapshot().getSize() + msg.getSrcSnapshot().getSize(), msg.getVolume().getSize());

                        String provision = getVolumeProvision(msg.getVolume().getUuid(), msg.getVolume().getInstallPath());
                        if (Objects.equals(msg.getDstSnapshot().getUuid(), volume.getUuid()) && provision == null) {
                            logger.debug("dstPath is volume and volume is ThickProvisioning, no need to extend");
                            trigger.next();
                            return;
                        }

                        ExtendLogicalVolumeCmd cmd = new ExtendLogicalVolumeCmd();
                        cmd.requiredSize = newSize;
                        cmd.volumeUuid = volume.getUuid();
                        cmd.destPath = convertInstallPathToAbsolute(msg.getDstSnapshot().getPrimaryStorageInstallPath());
                        cmd.vgUuid = volume.getPrimaryStorageUuid();
                        new Do(hostUuid, volume.getPrimaryStorageUuid()).go(EXTEND_LOGICAL_VOLUME_PATH, cmd, ExtendLogicalVolumeRsp.class, new ReturnValueCompletion<ExtendLogicalVolumeRsp>(trigger) {
                            @Override
                            public void success(ExtendLogicalVolumeRsp rsp) {
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
                    String __name__ = "offline-block-pull";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        OfflineMergeSnapshotCmd cmd = new OfflineMergeSnapshotCmd();
                        cmd.srcPath = msg.getSrcSnapshotParentPath() == null ? msg.getSrcSnapshotParentPath() : convertInstallPathToAbsolute(msg.getSrcSnapshotParentPath());
                        cmd.destPath = convertInstallPathToAbsolute(msg.getDstSnapshot().getPrimaryStorageInstallPath());
                        cmd.vgUuid = volume.getPrimaryStorageUuid();
                        cmd.volumeUuid = volume.getUuid();
                        cmd.fullRebase = cmd.srcPath == null;
                        new Do(hostUuid, cmd.vgUuid).go(OFFLINE_MERGE_SNAPSHOT_PATH, cmd, OfflineMergeSnapshotRsp.class, new ReturnValueCompletion<OfflineMergeSnapshotRsp>(completion) {
                            @Override
                            public void success(OfflineMergeSnapshotRsp returnValue) {
                                reply.setSize(returnValue.getActualSize());
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success(reply);
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void checkPrimaryStorageConsistency(ReturnValueCompletion<ConsistencyCheckResult> completion) {
        List<String> attachedClusterUuids = getSelfInventory().getAttachedClusterUuids();
        List<String> hostUuids = Q.New(HostVO.class)
                .notIn(HostVO_.state, asList(HostState.PreMaintenance, HostState.Maintenance))
                .in(HostVO_.clusterUuid, attachedClusterUuids)
                .select(HostVO_.uuid).listValues();
        if (hostUuids.isEmpty()) {
            completion.fail(operr("no hosts found in attached clusters, cannot check consistency"));
            return;
        }

        List<String> sharedBlockDiskUuids = Q.New(SharedBlockVO.class).eq(SharedBlockVO_.sharedBlockGroupUuid, self.getUuid())
                .select(SharedBlockVO_.diskUuid).listValues();
        Set<String> expectedWwids = sharedBlockDiskUuids.stream().map(String::toLowerCase).collect(Collectors.toSet());

        final ConsistencyCheckResult[] result = {null};

        new While<>(hostUuids).each((hostUuid, whileCompletion) -> {
            rescanAndQueryVgs(hostUuid, sharedBlockDiskUuids, new ReturnValueCompletion<GetManagedVgsInfoRsp>(whileCompletion) {
                @Override
                public void success(GetManagedVgsInfoRsp rsp) {
                    if (rsp.groupDiskInfos == null || rsp.groupDiskInfos.isEmpty()) {
                        logger.debug(String.format("no VG visible on host %s", hostUuid));
                        whileCompletion.done();
                        return;
                    }

                    ConsistencyCheckResult r = matchVgByWwid(rsp, expectedWwids, hostUuid);
                    if (r == null) {
                        whileCompletion.done();
                    } else if (!r.isSharedBlockGroupFound()) {
                        result[0] = r;
                        whileCompletion.done();
                    } else {
                        result[0] = r;
                        whileCompletion.allDone();
                    }
                }

                @Override
                public void fail(ErrorCode err) {
                    logger.debug(String.format("check consistency failed on host %s: %s", hostUuid, err));
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (result[0] != null) {
                    completion.success(result[0]);
                } else {
                    completion.fail(operr("failed to get valid vg lun info from all hosts, " +
                            "cannot check consistency of shared block group %s", self.getUuid()));
                }
            }
        });
    }

    private void rescanAndQueryVgs(String hostUuid, List<String> sharedBlockDiskUuids, ReturnValueCompletion<GetManagedVgsInfoRsp> completion) {
        SharedBlockKvmCommands.CheckDisksCmd checkCmd = new SharedBlockKvmCommands.CheckDisksCmd();
        checkCmd.sharedBlockUuids = sharedBlockDiskUuids;
        checkCmd.hostUuid = hostUuid;
        checkCmd.vgUuid = self.getUuid();
        checkCmd.rescan_scsi = true;
        httpCall(CHECK_DISKS_PATH, hostUuid, checkCmd, true, SharedBlockKvmCommands.AgentRsp.class,
                new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(completion) {
                    @Override
                    public void success(SharedBlockKvmCommands.AgentRsp ret) {
                        GetManagedVgsInfoCmd cmd = new GetManagedVgsInfoCmd();
                        httpCall(VGS_MANAGED_PATH, hostUuid, cmd, true, GetManagedVgsInfoRsp.class, completion);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    private ConsistencyCheckResult matchVgByWwid(GetManagedVgsInfoRsp rsp, Set<String> expectedWwids, String hostUuid) {
        boolean sawCompleteVg = false;
        for (Map.Entry<String, SharedBlockKvmCommands.SharedBlockGroupDiskInfo> entry : rsp.groupDiskInfos.entrySet()) {
            String vgUuid = entry.getKey();
            SharedBlockKvmCommands.SharedBlockGroupDiskInfo diskInfo = entry.getValue();
            List<SharedBlockCandidateStruct> luns = diskInfo.disks;

            if (luns == null || luns.stream().anyMatch(l -> l.getWwid() == null || l.getWwid().isEmpty())) {
                logger.debug(String.format("vgs info on host %s for VG %s has null/empty WWID, skipping", hostUuid, vgUuid));
                continue;
            }

            Long reportedCount = diskInfo.diskCount;
            if (reportedCount != null && reportedCount != (long) luns.size()) {
                logger.debug(String.format("vgs info on host %s for VG %s is incomplete: " +
                        "reported=%d, parsed=%d, skipping", hostUuid, vgUuid, reportedCount, luns.size()));
                continue;
            }

            Set<String> actualWwids = luns.stream().map(SharedBlockCandidateStruct::getWwid).map(String::toLowerCase).collect(Collectors.toSet());
            sawCompleteVg = true;

            if (!expectedWwids.equals(actualWwids)) {
                continue;
            }

            ConsistencyCheckResult result = new ConsistencyCheckResult();
            if (!Objects.equals(vgUuid, self.getUuid())) {
                result.setConsistent(false);
                result.setSharedBlockGroupFound(true);
                logger.info(String.format("VG %s found by WWID match on host %s, but UUID differs from database UUID %s (takeover candidate)",
                        vgUuid, hostUuid, self.getUuid()));
            } else {
                result.setConsistent(true);
                result.setSharedBlockGroupFound(true);
            }
            return result;
        }

        if (sawCompleteVg) {
            logger.debug(String.format("no VG on host %s has matching WWID set", hostUuid));
            ConsistencyCheckResult result = new ConsistencyCheckResult();
            result.setConsistent(false);
            result.setSharedBlockGroupFound(false);
            return result;
        }

        logger.debug(String.format("all VGs on host %s have incomplete WWID data, treating as unreliable", hostUuid));
        return null;
    }

    @Override
    void handle(UpdateVmInstanceMetadataOnPrimaryStorageMsg msg, final ReturnValueCompletion<UpdateVmInstanceMetadataOnPrimaryStorageReply> completion) {
        List<HostInventory> hosts = primaryStorageFactory.getConnectedHostsForOperation(getSelfInventory());
        if (hosts.isEmpty()) {
            completion.fail(operr("no connected host found for primary storage[uuid:%s]", msg.getPrimaryStorageUuid()));
            return;
        }

        String hostUuid = hosts.get(0).getUuid();
        WriteVmMetadataCmd cmd = new WriteVmMetadataCmd();
        cmd.vgUuid = self.getUuid();
        cmd.metadata = msg.getMetadata();
        cmd.metadataPath = msg.getMetadataPath();
        cmd.vmUuid = msg.getVmInstanceUuid();
        cmd.vmName = msg.getVmInstanceName();
        cmd.vmCategory = msg.getVmCategory();
        cmd.architecture = msg.getArchitecture();
        cmd.schemaVersion = msg.getSchemaVersion();
        new Do(hostUuid).go(WRITE_VM_METADATA_PATH, cmd, WriteVmMetadataRsp.class, new ReturnValueCompletion<WriteVmMetadataRsp>(completion) {
            @Override
            public void success(WriteVmMetadataRsp rsp) {
                completion.success(new UpdateVmInstanceMetadataOnPrimaryStorageReply());
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(GetVmInstanceMetadataFromPrimaryStorageMsg msg, final ReturnValueCompletion<GetVmInstanceMetadataFromPrimaryStorageReply> completion) {
        List<HostInventory> hosts = primaryStorageFactory.getConnectedHostsForOperation(getSelfInventory());
        if (hosts.isEmpty()) {
            completion.fail(operr("no connected host found for primary storage[uuid:%s]", self.getUuid()));
            return;
        }

        String hostUuid = hosts.get(0).getUuid();
        GetVmInstanceMetadataCmd cmd = new GetVmInstanceMetadataCmd();
        cmd.vgUuid = self.getUuid();
        cmd.setMetadataPath(msg.getMetadataPath());
        new Do(hostUuid).go(GET_VM_INSTANCE_METADATA_PATH, cmd, GetVmInstanceMetadataRsp.class, new ReturnValueCompletion<GetVmInstanceMetadataRsp>(completion) {
            @Override
            public void success(GetVmInstanceMetadataRsp rsp) {
                GetVmInstanceMetadataFromPrimaryStorageReply reply = new GetVmInstanceMetadataFromPrimaryStorageReply();
                reply.setMetadata(rsp.getMetadata());
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(ScanVmInstanceMetadataFromPrimaryStorageMsg msg, final ReturnValueCompletion<ScanVmInstanceMetadataFromPrimaryStorageReply> completion) {
        List<HostInventory> hosts = primaryStorageFactory.getConnectedHostsForOperation(getSelfInventory());
        if (hosts.isEmpty()) {
            completion.fail(operr("no connected host found for primary storage[uuid:%s]", self.getUuid()));
            return;
        }

        String hostUuid = hosts.get(0).getUuid();
        SharedBlockKvmCommands.ScanVmMetadataCmd cmd = new SharedBlockKvmCommands.ScanVmMetadataCmd();
        cmd.vgUuid = self.getUuid();
        cmd.metadataDir = msg.getMetadataDir();
        new Do(hostUuid).go(SCAN_VM_METADATA_PATH, cmd, SharedBlockKvmCommands.ScanVmMetadataRsp.class, new ReturnValueCompletion<SharedBlockKvmCommands.ScanVmMetadataRsp>(completion) {
            @Override
            public void success(SharedBlockKvmCommands.ScanVmMetadataRsp rsp) {
                ScanVmInstanceMetadataFromPrimaryStorageReply reply = new ScanVmInstanceMetadataFromPrimaryStorageReply();
                reply.setVmInstanceMetadata(rsp.getMetadataEntries());
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(CleanupVmInstanceMetadataOnPrimaryStorageMsg msg, final ReturnValueCompletion<CleanupVmInstanceMetadataOnPrimaryStorageReply> completion) {
        CleanupVmInstanceMetadataOnPrimaryStorageReply reply = new CleanupVmInstanceMetadataOnPrimaryStorageReply();

        List<HostInventory> hosts = primaryStorageFactory.getConnectedHostsForOperation(getSelfInventory());
        if (hosts.isEmpty()) {
            completion.fail(Platform.operr("no connected host found for primary storage[uuid:%s]", self.getUuid()));
            return;
        }

        String hostUuid = hosts.get(0).getUuid();
        CleanupVmMetadataCmd cmd = new CleanupVmMetadataCmd();
        cmd.vgUuid = self.getUuid();
        cmd.setMetadataPath(msg.getMetadataPath());
        new Do(hostUuid).go(CLEANUP_VM_METADATA_PATH, cmd, CleanupVmMetadataRsp.class, new ReturnValueCompletion<CleanupVmMetadataRsp>(completion) {
            @Override
            public void success(CleanupVmMetadataRsp rsp) {
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(RebaseVolumeBackingFileOnPrimaryStorageMsg msg, final ReturnValueCompletion<RebaseVolumeBackingFileOnPrimaryStorageReply> completion) {
        List<HostInventory> hosts = primaryStorageFactory.getConnectedHostsForOperation(getSelfInventory());
        if (hosts.isEmpty()) {
            completion.fail(Platform.operr("no connected host found for shared block primary storage[uuid:%s]", self.getUuid()));
            return;
        }
        String hostUuid = hosts.get(0).getUuid();

        List<String> absFilePaths = new ArrayList<>();
        for (String p : msg.getInstallPaths()) {
            absFilePaths.add(convertInstallPathToAbsolute(p));
        }

        SharedBlockKvmCommands.PrefixRebaseBackingFilesCmd cmd = new SharedBlockKvmCommands.PrefixRebaseBackingFilesCmd();
        cmd.filePaths = absFilePaths;
        cmd.oldPrefix = convertInstallPathToAbsolute(msg.getOldPrefix());
        cmd.newPrefix = convertInstallPathToAbsolute(msg.getNewPrefix());

        new Do(hostUuid).go(SharedBlockKvmCommands.PREFIX_REBASE_BACKING_FILES_PATH, cmd,
                SharedBlockKvmCommands.PrefixRebaseBackingFilesRsp.class,
                new ReturnValueCompletion<SharedBlockKvmCommands.PrefixRebaseBackingFilesRsp>(completion) {
                    @Override
                    public void success(SharedBlockKvmCommands.PrefixRebaseBackingFilesRsp rsp) {
                        RebaseVolumeBackingFileOnPrimaryStorageReply reply = new RebaseVolumeBackingFileOnPrimaryStorageReply();
                        reply.setRebasedCount(rsp.rebasedCount);
                        completion.success(reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }
}

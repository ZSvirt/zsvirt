package org.zstack.storage.zbs;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.cbd.*;
import org.zstack.cbd.kvm.CbdHeartbeatVolumeTO;
import org.zstack.cbd.kvm.CbdVolumeTo;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.HasThreadContext;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostAO_;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.addon.*;
import org.zstack.header.storage.addon.primary.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotStats;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.header.volume.VolumeStats;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMHostVO;
import org.zstack.kvm.KVMHostVO_;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.volume.VolumeGlobalConfig;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.multiErr;
import static org.zstack.core.Platform.operr;
import static org.zstack.storage.zbs.ZbsHelper.*;

/**
 * @author Xingwei Yu
 * @date 2024/3/21 13:11
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ZbsStorageController implements PrimaryStorageControllerSvc, PrimaryStorageNodeSvc {
    private static final CLogger logger = Utils.getLogger(ZbsStorageController.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;

    private ExternalPrimaryStorageVO self;
    private AddonInfo addonInfo;
    private Config config;

    public static final String DEPLOY_CLIENT_PATH = "/zbs/primarystorage/client/deploy";
    public static final String GET_CAPACITY_PATH = "/zbs/primarystorage/capacity";
    public static final String GET_FACTS_PATH = "/zbs/primarystorage/facts";
    public static final String COPY_PATH = "/zbs/primarystorage/copy";
    public static final String CREATE_VOLUME_PATH = "/zbs/primarystorage/volume/create";
    public static final String DELETE_VOLUME_PATH = "/zbs/primarystorage/volume/delete";
    public static final String CLONE_VOLUME_PATH = "/zbs/primarystorage/volume/clone";
    public static final String QUERY_VOLUME_PATH = "/zbs/primarystorage/volume/query";
    public static final String BATCH_QUERY_VOLUME_PATH = "/zbs/primarystorage/volume/query/batch";
    public static final String EXPAND_VOLUME_PATH = "/zbs/primarystorage/volume/expand";
    public static final String FLATTEN_VOLUME_PATH = "/zbs/primarystorage/volume/flatten";
    public static final String CBD_TO_NBD_PATH = "/zbs/primarystorage/volume/cbdtonbd";
    public static final String CLEAN_NBD_PATH = "/zbs/primarystorage/volume/cleannbd";
    public static final String CREATE_SNAPSHOT_PATH = "/zbs/primarystorage/snapshot/create";
    public static final String DELETE_SNAPSHOT_PATH = "/zbs/primarystorage/snapshot/delete";
    public static final String ROLLBACK_SNAPSHOT_PATH = "/zbs/primarystorage/snapshot/rollback";
    public static final String CHECK_HOST_STORAGE_CONNECTION_PATH = "/zbs/primarystorage/check/host/connection";
    public static final String GET_VOLUME_CLIENTS_PATH = "/zbs/primarystorage/volume/clients";

    private static final StorageCapabilities capabilities = new StorageCapabilities();

    static {
        VolumeSnapshotCapability scap = new VolumeSnapshotCapability();
        scap.setSupport(true);
        scap.setArrangementType(VolumeSnapshotCapability.VolumeSnapshotArrangementType.INDIVIDUAL);
        scap.setSupportCreateOnHypervisor(false);
        scap.setSupportLazyDelete(false);
        scap.setVolumePathFromInnerSnapshotRegex("^[^@]+");
        capabilities.setSnapshotCapability(scap);
        capabilities.setSupportShareableVolume(true);
        capabilities.setSupportCloneFromVolume(false);
        capabilities.setSupportStorageQos(false);
        capabilities.setSupportLiveExpandVolume(false);
        capabilities.setSupportedImageFormats(Collections.singletonList(ImageConstant.RAW_FORMAT_STRING));
        capabilities.setDefaultIsoActiveProtocol(VolumeProtocol.CBD);
        capabilities.setDefaultImageExportProtocol(VolumeProtocol.NBD);
    }

    @Override
    public void activate(BaseVolumeInfo v, HostInventory h, boolean shareable, ReturnValueCompletion<ActiveVolumeTO> comp) {
        if (VolumeProtocol.CBD.toString().equals(v.getProtocol())) {
            comp.success(new CbdVolumeTo());
            return;
        }

        comp.fail(operr("not supported protocol[%s]", v.getProtocol()));
    }

    @Override
    public void deactivate(String installPath, String protocol, HostInventory h, Completion comp) {
        // not support inactive client yet
        comp.success();
    }

    @Override
    public void deactivate(String installPath, String protocol, ActiveVolumeClient client, Completion comp) {
        comp.success();
    }

    @Override
    public void blacklist(String installPath, String protocol, HostInventory h, Completion comp) {
        comp.success();
    }

    @Override
    public String getActivePath(BaseVolumeInfo v, HostInventory h, boolean shareable) {
        return null;
    }

    @Override
    public BaseVolumeInfo getActiveVolumeInfo(String activePath, HostInventory h, boolean shareable) {
        return null;
    }

    @Override
    public List<ActiveVolumeClient> getActiveClients(String installPath, String protocol) {
        if (VolumeProtocol.CBD.toString().equals(protocol)) {
            GetVolumeClientsCmd cmd = new GetVolumeClientsCmd();
            cmd.setPath(installPath);
            GetVolumeClientsRsp rsp = syncHttpCall(GET_VOLUME_CLIENTS_PATH, cmd, GetVolumeClientsRsp.class);
            List<ActiveVolumeClient> clients = new ArrayList<>();

            if (!rsp.isSuccess()) {
                throw new OperationFailureException(operr(rsp.getError()));
            }

            if (rsp.getClients() != null) {
                for (ClientInfo clientInfo : rsp.getClients()) {
                    ActiveVolumeClient client = new ActiveVolumeClient();
                    client.setManagerIp(clientInfo.getIp());
                    clients.add(client);
                }
            }
            return clients;
        } else {
            throw new OperationFailureException(operr("not supported protocol[%s] for active", protocol));
        }
    }

    @Override
    public List<String> getActiveVolumesLocation(HostInventory h) {
        return null;
    }

    @Override
    public void deployClient(HostInventory h, Completion comp) {
        KVMHostVO host = org.zstack.core.db.Q.New(KVMHostVO.class).eq(KVMHostVO_.uuid, h.getUuid()).find();
        if (host == null) {
            comp.fail(operr("cannot found kvm host[uuid:%s], unable to deploy client", h.getUuid()));
            return;
        }

        DeployClientCmd cmd = new DeployClientCmd();
        cmd.setIp(h.getManagementIp());
        cmd.setPort(host.getPort());
        cmd.setUsername(host.getUsername());
        cmd.setPassword(host.getPassword());
        httpCall(DEPLOY_CLIENT_PATH, cmd, DeployClientRsp.class, new ReturnValueCompletion<DeployClientRsp>(comp) {
            @Override
            public void success(DeployClientRsp returnValue) {
                comp.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public synchronized void activateHeartbeatVolume(HostInventory h, ReturnValueCompletion<HeartbeatVolumeTO> comp) {
        if (config == null) {
            reloadDbInfo();
        }

        CreateVolumeCmd cmd = new CreateVolumeCmd();
        cmd.setLogicalPool(config.getLogicalPoolName());
        cmd.setVolume(ZbsConstants.ZBS_HEARTBEAT_VOLUME_NAME);
        cmd.setSize(ZbsConstants.ZBS_HEARTBEAT_VOLUME_SIZE_IN_GIGABYTE);
        cmd.setSkipIfExisting(true);

        httpCall(CREATE_VOLUME_PATH, cmd, CreateVolumeRsp.class, new ReturnValueCompletion<CreateVolumeRsp>(comp) {
            @Override
            public void success(CreateVolumeRsp returnValue) {
                CbdHeartbeatVolumeTO to = new CbdHeartbeatVolumeTO();
                to.setInstallPath(returnValue.installPath);
                to.setHeartbeatRequiredSpace(SizeUnit.MEGABYTE.toByte(1));
                to.setCoveringPaths(Collections.singletonList(config.getLogicalPoolName()));
                comp.success(to);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void deactivateHeartbeatVolume(HostInventory h, Completion comp) {
        comp.success();
    }

    @Override
    public HeartbeatVolumeTO getHeartbeatVolumeActiveInfo(HostInventory h) {
        if (config == null) {
            reloadDbInfo();
        }

        CbdHeartbeatVolumeTO to = new CbdHeartbeatVolumeTO();
        to.setInstallPath(buildHeartbeatVolumePath(config.getLogicalPoolName()));
        to.setHeartbeatRequiredSpace(SizeUnit.MEGABYTE.toByte(1));
        to.setCoveringPaths(Collections.singletonList(config.getLogicalPoolName()));

        return to;
    }

    @Override
    public String getIdentity() {
        return ZbsConstants.IDENTITY;
    }

    @Override
    public void connect(String cfg, String url, ReturnValueCompletion<LinkedHashMap> completion) {
        AddonInfo newAddonInfo = new AddonInfo();
        Config current = JSONObjectUtil.toObject(cfg, Config.class);
        List<MdsInfo> mdsInfos = parseMdsInfos(current.getMdsUrls());
        newAddonInfo.setMdsInfos(mdsInfos);
        final List<ZbsPrimaryStorageMdsBase> mdsList = CollectionUtils.transformAndRemoveNull(newAddonInfo.getMdsInfos(),
                ZbsPrimaryStorageMdsBase::new);

        class Connector {
            private final ErrorCodeList errorCodes = new ErrorCodeList();
            private final Iterator<ZbsPrimaryStorageMdsBase> it = mdsList.iterator();

            void connect(final FlowTrigger trigger) {
                if (!it.hasNext()) {
                    if (errorCodes.size() == mdsList.size()) {
                        if (errorCodes.isEmpty()) {
                            trigger.fail(operr("unable to connect to the ZBS primary storage[uuid:%s]," +
                                    " failed to connect all MDS", self.getUuid()));
                        } else {
                            trigger.fail(multiErr(errorCodes, "unable to connect to the ZBS primary storage[uuid:%s]," +
                                            " failed to connect all MDS",
                                    self.getUuid()));
                        }
                    } else {
                        ExternalPrimaryStorageVO vo = dbf.reload(self);
                        if (vo == null) {
                            trigger.fail(operr("ZBS primary storage[uuid:%s] may have been deleted", self.getUuid()));
                        } else {
                            self = vo;
                            trigger.next();
                        }
                    }
                    return;
                }

                final ZbsPrimaryStorageMdsBase base = it.next();
                base.connect(new Completion(trigger) {
                    @Override
                    public void success() {
                        connect(trigger);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        errorCodes.add(errorCode);
                        connect(trigger);
                    }
                });
            }
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("connect-zbs-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "connect-mds";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new Connector().connect(trigger);
                        addonInfo = newAddonInfo;
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "get-facts";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        httpCall(GET_FACTS_PATH, new GetFactsCmd(), GetFactsRsp.class, new ReturnValueCompletion<GetFactsRsp>(trigger) {
                            @Override
                            public void success(GetFactsRsp returnValue) {
                                ClusterInfo info = new ClusterInfo();
                                info.setUuid(returnValue.getUuid());
                                info.setVersion(returnValue.getVersion());
                                newAddonInfo.setClusterInfo(info);
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
                    String __name__ = "deploy-client";

                    List<PrimaryStorageClusterRefVO> refs = org.zstack.core.db.Q.New(PrimaryStorageClusterRefVO.class)
                            .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, self.getUuid())
                            .list();

                    @Override
                    public boolean skip(Map data) {
                        return refs.isEmpty();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> clusterUuids = refs.stream()
                                .map(PrimaryStorageClusterRefVO::getClusterUuid)
                                .collect(Collectors.toList());

                        List<HostVO> hosts = org.zstack.core.db.Q.New(HostVO.class)
                                .in(HostAO_.clusterUuid, clusterUuids)
                                .list();

                        new While<>(hosts).each((h, comp) -> {
                            KVMHostVO host = org.zstack.core.db.Q.New(KVMHostVO.class).eq(KVMHostVO_.uuid, h.getUuid()).find();
                            if (host == null) {
                                comp.addError(operr("cannot found kvm host[uuid:%s], unable to deploy client", h.getUuid()));
                                comp.allDone();
                                return;
                            }

                            DeployClientCmd cmd = new DeployClientCmd();
                            cmd.setIp(h.getManagementIp());
                            cmd.setPort(host.getPort());
                            cmd.setUsername(host.getUsername());
                            cmd.setPassword(host.getPassword());
                            httpCall(DEPLOY_CLIENT_PATH, cmd, DeployClientRsp.class, new ReturnValueCompletion<DeployClientRsp>(comp) {
                                @Override
                                public void success(DeployClientRsp returnValue) {
                                    comp.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    comp.addError(errorCode);
                                    comp.allDone();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
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
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        configUrl(self.getUuid());
                        addonInfo = newAddonInfo;
                        completion.success(JSONObjectUtil.rehashObject(newAddonInfo, LinkedHashMap.class));
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
    public void ping(Completion completion) {
        reloadDbInfo();

        if (addonInfo == null || addonInfo.getClusterInfo() == null) {
            completion.fail(operr(String.format("addon info is null, primary storage[uuid:%s] is not ready, skip ping task", self.getUuid())));
            return;
        }

        final List<ZbsPrimaryStorageMdsBase> mds = CollectionUtils.transform(addonInfo.getMdsInfos(), ZbsPrimaryStorageMdsBase::new);
        new While<>(mds).each((m, comp) -> m.ping(addonInfo.getClusterInfo(), new Completion(comp) {
            @Override
            public void success() {
                m.getSelf().setStatus(MdsStatus.Connected);
                comp.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                m.getSelf().setStatus(MdsStatus.Disconnected);
                comp.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                SQL.New(ExternalPrimaryStorageVO.class).eq(ExternalPrimaryStorageVO_.uuid, self.getUuid())
                        .set(ExternalPrimaryStorageVO_.addonInfo, JSONObjectUtil.toJsonString(addonInfo))
                        .update();

                boolean isConnected = addonInfo.getMdsInfos().stream().anyMatch(mdsInfo -> MdsStatus.Connected.equals(mdsInfo.getStatus()));
                if (!isConnected) {
                    String notConnectedIps = addonInfo.getMdsInfos().stream()
                            .filter(mdsInfo -> !MdsStatus.Connected.equals(mdsInfo.getStatus()))
                            .map(MdsInfo::getAddr)
                            .collect(Collectors.joining(", "));

                    completion.fail(operr("no MDS is Connected, the following MDS[%s] are not Connected.", notConnectedIps));
                    return;
                }
                completion.success();
            }
        });
    }

    @Override
    public void reportCapacity(ReturnValueCompletion<StorageCapacity> comp) {
        reloadDbInfo();

        GetCapacityCmd cmd = new GetCapacityCmd();
        cmd.setLogicalPool(config.getLogicalPoolName());

        httpCall(GET_CAPACITY_PATH, cmd, GetCapacityRsp.class, new ReturnValueCompletion<GetCapacityRsp>(comp) {
            @Override
            public void success(GetCapacityRsp returnValue) {
                addonInfo.setLogicalPoolInfos(returnValue.getLogicalPoolInfos());
                SQL.New(ExternalPrimaryStorageVO.class).eq(ExternalPrimaryStorageVO_.uuid, self.getUuid())
                        .set(ExternalPrimaryStorageVO_.addonInfo, JSONObjectUtil.toJsonString(addonInfo))
                        .update();

                List<LogicalPoolInfo> logicalPoolInfos = getSelfPools();
                long total = logicalPoolInfos.stream().mapToLong(LogicalPoolInfo::getCapacity).sum();
                long used = logicalPoolInfos.stream().mapToLong(LogicalPoolInfo::getUsedSize).sum();
                long avail = total != 0 ? total - used : 0;

                StorageCapacity cap = new StorageCapacity();
                cap.setTotalCapacity(total);
                cap.setAvailableCapacity(avail);
                cap.setHealthy(StorageHealthy.Ok);
                comp.success(cap);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void reportHealthy(ReturnValueCompletion<StorageHealthy> comp) {

    }

    @Override
    public void reportNodeHealthy(HostInventory host, ReturnValueCompletion<NodeHealthy> comp) {
        CheckHostStorageConnectionCmd cmd = new CheckHostStorageConnectionCmd();
        cmd.setHostUuid(host.getUuid());
        cmd.setPath(buildHeartbeatVolumePath(config.getLogicalPoolName()));

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(host.getUuid());
        msg.setPath(CHECK_HOST_STORAGE_CONNECTION_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(comp) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    comp.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                CheckHostStorageConnectionRsp rsp = hreply.toResponse(CheckHostStorageConnectionRsp.class);
                NodeHealthy healthy = new NodeHealthy();
                healthy.setHealthy(VolumeProtocol.CBD, rsp.isSuccess() ? StorageHealthy.Ok : StorageHealthy.Failed);
                comp.success(healthy);
            }
        });
    }

    @Override
    public StorageCapabilities reportCapabilities() {
        return capabilities;
    }

    @Override
    public String allocateSpace(AllocateSpaceSpec aspec) {
        if (config == null || addonInfo == null) {
            reloadDbInfo();
        }

        // TODO allocate pool
        LogicalPoolInfo logicalPoolInfo = allocateFreePool(aspec.getSize());
        if (logicalPoolInfo == null) {
            throw new OperationFailureException(operr("no available logical pool with enough space[%d]", aspec.getSize()));
        }

        return buildVolumePath("", config.getLogicalPoolName(), "");
    }

    private LogicalPoolInfo allocateFreePool(long size) {
        List<LogicalPoolInfo> logicalPoolInfos = getSelfPools();
        return logicalPoolInfos.stream().filter(it -> it.getCapacity() - it.getUsedSize() > size)
                .max(Comparator.comparingLong(it -> it.getCapacity() - it.getUsedSize()))
                .orElse(null);
    }

    private List<LogicalPoolInfo> getSelfPools() {
        String configLogicalPoolName = config.getLogicalPoolName();
        List<LogicalPoolInfo> logicalPoolInfos = addonInfo.getLogicalPoolInfos();
        logicalPoolInfos.removeIf(it -> !configLogicalPoolName.equals(it.getLogicalPoolName()));
        return logicalPoolInfos;
    }

    private ZbsVolumeEncryptionExtensionPoint getVolumeEncryptionExtension() {
        List<ZbsVolumeEncryptionExtensionPoint> exts = pluginRgty.getExtensionList(ZbsVolumeEncryptionExtensionPoint.class);
        if (exts.isEmpty()) {
            throw new OperationFailureException(operr("cannot find ZBS volume encryption extension"));
        }
        return exts.get(0);
    }

    private ZbsVolumeEncryptionBackend encryptionBackend() {
        if (config == null || addonInfo == null) {
            reloadDbInfo();
        }
        return new ZbsVolumeEncryptionBackendImpl(this, self.getUuid(), addonInfo, config);
    }

    @Override
    public void createVolume(CreateVolumeSpec v, ReturnValueCompletion<VolumeStats> comp) {
        reloadDbInfo();

        if (v.isEncrypted()) {
            try {
                getVolumeEncryptionExtension().createEncryptedEmptyVolume(encryptionBackend(), v, comp);
            } catch (OperationFailureException e) {
                comp.fail(e.getErrorCode());
            }
            return;
        }

        CreateVolumeCmd cmd = new CreateVolumeCmd();
        cmd.setLogicalPool(config.getLogicalPoolName());
        cmd.setVolume(v.getName());
        cmd.setUnit(getSizeUnit(addonInfo.getClusterInfo().getVersion()));
        cmd.setSize(alignSizeTo(v.getSize(), cmd.getUnit()));
        cmd.setSkipIfExisting(true);

        httpCall(CREATE_VOLUME_PATH, cmd, CreateVolumeRsp.class, new ReturnValueCompletion<CreateVolumeRsp>(comp) {
            @Override
            public void success(CreateVolumeRsp returnValue) {
                VolumeStats stats = new VolumeStats();
                stats.setInstallPath(returnValue.installPath);
                stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                stats.setSize(returnValue.getSize());
                stats.setActualSize(returnValue.getActualSize());
                comp.success(stats);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void deleteVolume(String installPath, Completion comp) {
        doDeleteVolume(installPath, true, comp);
    }

    @Override
    public void deleteVolumeAndSnapshot(String installPath, Completion comp) {
        doDeleteVolume(installPath, true, comp);
    }

    @Override
    public void trashVolume(String installPath, Completion comp) {
        doDeleteVolume(installPath, false, comp);
    }

    @Override
    public void cloneVolume(String srcInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats> comp) {
        if (dst.isEncrypted()) {
            try {
                getVolumeEncryptionExtension().cloneEncryptedVolumeFromImage(encryptionBackend(), srcInstallPath, dst, comp);
            } catch (OperationFailureException e) {
                comp.fail(e.getErrorCode());
            }
            return;
        }

        VolumeStats stats = new VolumeStats();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("clone-volume-%s", dst.getName()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "clone-volume";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CloneVolumeCmd cmd = new CloneVolumeCmd();
                        cmd.setPath(srcInstallPath);
                        cmd.setDstVolume(dst.getName());

                        httpCall(CLONE_VOLUME_PATH, cmd, CloneVolumeRsp.class, new ReturnValueCompletion<CloneVolumeRsp>(trigger) {
                            @Override
                            public void success(CloneVolumeRsp returnValue) {
                                stats.setInstallPath(returnValue.installPath);
                                stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                                stats.setSize(returnValue.getSize());
                                stats.setActualSize(returnValue.getActualSize());
                                stats.setParentUri(srcInstallPath);
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
                    String __name__ = "resize-volume";

                    @Override
                    public boolean skip(Map data) {
                        return stats.getSize() >= dst.getSize();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ExpandVolumeCmd cmd = new ExpandVolumeCmd();
                        cmd.setPath(stats.getInstallPath());
                        cmd.setUnit(getSizeUnit(addonInfo.getClusterInfo().getVersion()));
                        cmd.setSize(alignSizeTo(dst.getSize(), cmd.getUnit()));

                        httpCall(EXPAND_VOLUME_PATH, cmd, ExpandVolumeRsp.class, new ReturnValueCompletion<ExpandVolumeRsp>(trigger) {
                            @Override
                            public void success(ExpandVolumeRsp returnValue) {
                                stats.setSize(returnValue.getSize());
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(comp) {
                    @Override
                    public void handle(Map data) {
                        comp.success(stats);
                    }
                });

                error(new FlowErrorHandler(comp) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        comp.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void copyVolume(String srcInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats> comp) {
        if (dst.isEncrypted()) {
            try {
                getVolumeEncryptionExtension().copyEncryptedVolumeFromSnapshot(encryptionBackend(), srcInstallPath, dst, comp);
            } catch (OperationFailureException e) {
                comp.fail(e.getErrorCode());
            }
            return;
        }

        CopyCmd cmd = new CopyCmd();
        cmd.setPath(srcInstallPath);
        cmd.setDstVolume(dst.getName());
        cmd.setDstSize(dst.getSize() / (1L << 30));

        httpCall(COPY_PATH, cmd, CopyRsp.class, new ReturnValueCompletion<CopyRsp>(comp) {
            @Override
            public void success(CopyRsp returnValue) {
                VolumeStats stats = new VolumeStats();
                stats.setInstallPath(returnValue.getInstallPath());
                stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                stats.setSize(returnValue.getSize());
                stats.setActualSize(returnValue.getActualSize());
                comp.success(stats);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void encryptVolumeBits(EncryptVolumeBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<EncryptVolumeBitsOnPrimaryStorageReply> comp) {
        try {
            getVolumeEncryptionExtension().encryptVolumeBits(encryptionBackend(), msg, comp);
        } catch (OperationFailureException e) {
            comp.fail(e.getErrorCode());
        }
    }

    @Override
    public void convertVolumeEncryption(ConvertVolumeEncryptionOnPrimaryStorageMsg msg,
                                        ReturnValueCompletion<ConvertVolumeEncryptionOnPrimaryStorageReply> completion) {
        try {
            getVolumeEncryptionExtension().convertVolumeEncryption(encryptionBackend(), msg, completion);
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
        }
    }

    @Override
    public void flattenVolume(String installPath, ReturnValueCompletion<VolumeStats> comp) {
        FlattenVolumeCmd cmd = new FlattenVolumeCmd();
        cmd.setPath(installPath);

        httpCall(FLATTEN_VOLUME_PATH, cmd, FlattenVolumeRsp.class, new ReturnValueCompletion<FlattenVolumeRsp>(comp) {
            @Override
            public void success(FlattenVolumeRsp returnValue) {
                VolumeStats stats = new VolumeStats();
                stats.setInstallPath(installPath);
                stats.setSize(returnValue.getSize());
                stats.setActualSize(returnValue.getActualSize());
                stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                comp.success(stats);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void stats(String installPath, ReturnValueCompletion<VolumeStats> comp) {
        QueryVolumeCmd cmd = new QueryVolumeCmd();
        cmd.setPath(installPath);

        httpCall(QUERY_VOLUME_PATH, cmd, QueryVolumeRsp.class, new ReturnValueCompletion<QueryVolumeRsp>(comp) {
            @Override
            public void success(QueryVolumeRsp returnValue) {
                VolumeStats stats = new VolumeStats();
                stats.setInstallPath(installPath);
                stats.setSize(returnValue.getSize());
                stats.setActualSize(returnValue.getActualSize());
                stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                stats.setParentUri(returnValue.getParentUri());
                comp.success(stats);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void batchStats(Collection<String> installPath, ReturnValueCompletion<List<VolumeStats>> comp) {
        BatchQueryVolumeCmd cmd = new BatchQueryVolumeCmd();
        cmd.setInstallPaths(installPath);

        httpCall(BATCH_QUERY_VOLUME_PATH, cmd, BatchQueryVolumeRsp.class, new ReturnValueCompletion<BatchQueryVolumeRsp>(comp) {
            @Override
            public void success(BatchQueryVolumeRsp returnValue) {
                List<VolumeStats> stats = returnValue.getVolumes().entrySet().stream().map(v -> {
                    VolumeStats s = new VolumeStats();
                    s.setInstallPath(v.getKey());
                    s.setSize(v.getValue().get("length"));
                    s.setActualSize(v.getValue().get("usedSize"));
                    s.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                    return s;
                }).collect(Collectors.toList());
                comp.success(stats);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void expandVolume(VolumeInventory volume, long size, ReturnValueCompletion<VolumeStats> comp) {
        if (Boolean.TRUE.equals(volume.getEncrypted())) {
            List<ZbsVolumeEncryptionExtensionPoint> exts = pluginRgty.getExtensionList(ZbsVolumeEncryptionExtensionPoint.class);
            if (exts.isEmpty()) {
                comp.fail(operr("cannot find ZBS volume encryption extension"));
                return;
            }
            exts.get(0).resizeEncryptedVolume(self.getUuid(), volume, size, comp);
            return;
        }

        String installPath = volume.getInstallPath();
        ExpandVolumeCmd cmd = new ExpandVolumeCmd();
        cmd.setPath(installPath);
        cmd.setUnit(getSizeUnit(addonInfo.getClusterInfo().getVersion()));
        cmd.setSize(alignSizeTo(size, cmd.getUnit()));

        httpCall(EXPAND_VOLUME_PATH, cmd, ExpandVolumeRsp.class, new ReturnValueCompletion<ExpandVolumeRsp>(comp) {
            @Override
            public void success(ExpandVolumeRsp returnValue) {
                VolumeStats stats = new VolumeStats();
                stats.setInstallPath(installPath);
                stats.setSize(returnValue.getSize());
                comp.success(stats);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void setVolumeQos(BaseVolumeInfo v, Completion comp) {
        comp.success();
    }

    @Override
    public void deleteVolumeQos(BaseVolumeInfo v, Completion comp) {
        comp.success();
    }

    @Override
    public void export(ExportSpec espec, VolumeProtocol protocol, ReturnValueCompletion<RemoteTarget> comp) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            NbdRemoteTarget target = new NbdRemoteTarget();
            target.setIp("127.0.0.1");
            target.setPort(6666);
            comp.success(target);
            return;
        }

        if (protocol != VolumeProtocol.NBD) {
            comp.fail(operr("unsupported protocol %s", protocol.name()));
            return;
        }

        CbdToNbdCmd cmd = new CbdToNbdCmd();
        cmd.setPath(espec.getInstallPath());
        cmd.setPortRange(HostGlobalConfig.NBD_PORT_RANGE.value(String.class));

        httpCall(CBD_TO_NBD_PATH, cmd, CbdToNbdRsp.class, new ReturnValueCompletion<CbdToNbdRsp>(comp) {
            @Override
            public void success(CbdToNbdRsp returnValue) {
                NbdRemoteTarget target = new NbdRemoteTarget();
                target.setIp(returnValue.getIp());
                target.setPort(returnValue.getPort());
                comp.success(target);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void unexport(ExportSpec espec, RemoteTarget remoteTarget, VolumeProtocol protocol, Completion comp) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            comp.success();
            return;
        }

        if (protocol != VolumeProtocol.NBD) {
            comp.fail(operr("unsupported protocol %s", protocol.name()));
            return;
        }

        if (remoteTarget == null || remoteTarget.getResourceURI() == null) {
            logger.debug("remote target or the URI does not exist");
            comp.success();
            return;
        }

        try {
            URI uri = new URI(remoteTarget.getResourceURI());

            CleanNbdCmd cmd = new CleanNbdCmd();
            cmd.setNbdAddr(uri.getHost());
            cmd.setPort(uri.getPort());
            new HttpCaller<>(CLEAN_NBD_PATH, cmd, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(comp) {
                @Override
                public void success(AgentResponse returnValue) {
                    comp.success();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    comp.fail(errorCode);
                }
            }).setTargetMds(uri.getHost()).call();
        } catch (URISyntaxException e) {
            comp.fail(operr("invalid URI syntax: %s", e.getMessage()));
        }
    }

    @Override
    public void createSnapshot(CreateVolumeSnapshotSpec spec, ReturnValueCompletion<VolumeSnapshotStats> comp) {
        CreateSnapshotCmd cmd = new CreateSnapshotCmd();
        cmd.setPath(spec.getVolumeInstallPath());
        cmd.setSnapshot(spec.getName());
        cmd.setSkipOnExisting(true);

        httpCall(CREATE_SNAPSHOT_PATH, cmd, CreateSnapshotRsp.class, new ReturnValueCompletion<CreateSnapshotRsp>(comp) {
            @Override
            public void success(CreateSnapshotRsp returnValue) {
                VolumeSnapshotStats stats = new VolumeSnapshotStats();
                stats.setInstallPath(returnValue.getInstallPath());
                stats.setActualSize(returnValue.getActualSize());
                comp.success(stats);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void deleteSnapshot(String installPath, Completion comp) {
        DeleteSnapshotCmd cmd = new DeleteSnapshotCmd();
        cmd.setPath(installPath);

        httpCall(DELETE_SNAPSHOT_PATH, cmd, DeleteSnapshotRsp.class, new ReturnValueCompletion<DeleteSnapshotRsp>(comp) {
            @Override
            public void success(DeleteSnapshotRsp returnValue) {
                comp.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void expungeSnapshot(String installPath, Completion comp) {
        comp.success();
    }

    @Override
    public void revertVolumeSnapshot(String snapshotInstallPath, ReturnValueCompletion<VolumeStats> comp) {
        RollbackSnapshotCmd cmd = new RollbackSnapshotCmd();
        cmd.setPath(snapshotInstallPath);

        httpCall(ROLLBACK_SNAPSHOT_PATH, cmd, RollbackSnapshotRsp.class, new ReturnValueCompletion<RollbackSnapshotRsp>(comp) {
            @Override
            public void success(RollbackSnapshotRsp returnValue) {
                VolumeStats stats = new VolumeStats();
                stats.setInstallPath(returnValue.getInstallPath());
                stats.setSize(returnValue.getSize());
                stats.setActualSize(returnValue.getActualSize());
                stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                comp.success(stats);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void validateConfig(String config) {
        Config old = JSONObjectUtil.toObject(self.getConfig(), Config.class);
        Config current = JSONObjectUtil.toObject(config, Config.class);

        if (current.getLogicalPoolName().contains("/")) {
            throw new CloudRuntimeException(String.format("invalid logical pool name[%s]", current.getLogicalPoolName()));
        }

        if (current.getMdsUrls().isEmpty()) {
            throw new OperationFailureException(operr("ensure at least one MDS is configured"));
        }

        List<MdsInfo> newMdsInfos = parseMdsInfos(current.getMdsUrls());
        List<MdsInfo> duplicateMdsInfos = newMdsInfos.stream().collect(Collectors.groupingBy(MdsInfo::getAddr))
                .values().stream().filter(addr -> addr.size() > 1).flatMap(List::stream).collect(Collectors.toList());
        if (!duplicateMdsInfos.isEmpty()) {
            throw new OperationFailureException(operr("do not allow to add duplicate MDS[%s]",
                    duplicateMdsInfos.stream().map(MdsInfo::getAddr).distinct().collect(Collectors.joining(", "))
            ));
        }

        List<MdsInfo> oldMdsInfos = parseMdsInfos(old.getMdsUrls());
        List<MdsInfo> changedMdsInfos = newMdsInfos.stream().filter(n -> oldMdsInfos.stream().noneMatch(o -> o.equals(n))).collect(Collectors.toList());
        if (!changedMdsInfos.isEmpty() && !CoreGlobalProperty.UNIT_TEST_ON) {
            List<ZbsPrimaryStorageMdsBase> mdsList = CollectionUtils.transformAndRemoveNull(changedMdsInfos, ZbsPrimaryStorageMdsBase::new);
            for (ZbsPrimaryStorageMdsBase base : mdsList) {
                base.checkSshAndTools();
                base.checkStorageHealth();
            }
        }
    }

    @Override
    public void setTrashExpireTime(int timeInSeconds, Completion completion) {
        completion.success();
    }

    @Override
    public void onFirstAdditionConfigure(Completion completion) {
        ResourceConfig rc = rcf.getResourceConfig(VolumeGlobalConfig.VOLUME_PHYSICAL_BLOCK_SIZE.getIdentity());
        rc.updateValue(self.getUuid(), ZbsConstants.VOLUME_PHYSICAL_BLOCK_SIZE);
        completion.success();
    }

    @Override
    public long alignSize(long size) {
        String unit = getSizeUnit(addonInfo.getClusterInfo().getVersion());
        return convertSizeToByte(alignSizeTo(size, unit), unit);
    }

    public void doDeleteVolume(String installPath, Boolean force, Completion comp) {
        DeleteVolumeCmd cmd = new DeleteVolumeCmd();
        cmd.setPath(installPath);
        cmd.setForce(force);

        httpCall(DELETE_VOLUME_PATH, cmd, DeleteVolumeRsp.class, new ReturnValueCompletion<DeleteVolumeRsp>(comp) {
            @Override
            public void success(DeleteVolumeRsp returnValue) {
                comp.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    private void reloadDbInfo() {
        self = dbf.reload(self);
        addonInfo = StringUtils.isEmpty(self.getAddonInfo()) ? new AddonInfo() : JSONObjectUtil.toObject(self.getAddonInfo(), AddonInfo.class);
        config = StringUtils.isEmpty(self.getConfig()) ? new Config() : JSONObjectUtil.toObject(self.getConfig(), Config.class);
    }

    private List<MdsInfo> parseMdsInfos(List<String> mdsUrls) {
        return mdsUrls.stream().map(mdsUrl -> {
            MdsUri uri = new MdsUri(mdsUrl);
            MdsInfo mdsInfo = new MdsInfo();
            mdsInfo.setUsername(uri.getUsername());
            mdsInfo.setPassword(uri.getPassword());
            mdsInfo.setPort(uri.getSshPort());
            mdsInfo.setAddr(uri.getHostname());
            return mdsInfo;
        }).collect(Collectors.toList());
    }

    protected <T extends AgentResponse> T syncHttpCall(final String path, final AgentCommand cmd, final Class<T> retClass) {
        return httpCall(path, cmd, retClass, null, 0, true);
    }

    protected <T extends AgentResponse> T httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, TimeUnit unit, long timeout, boolean sync) {
        return new HttpCaller<>(path, cmd, retClass, null, unit, timeout, sync).syncCall();
    }

    protected <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, final ReturnValueCompletion<T> callback) {
        httpCall(path, cmd, retClass, callback, null, 0, false);
    }

    protected <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, final ReturnValueCompletion<T> callback, TimeUnit unit, long timeout, boolean sync) {
        new HttpCaller<>(path, cmd, retClass, callback, unit, timeout, sync).call();
    }

    public class HttpCaller<T extends AgentResponse> {
        private Iterator<ZbsPrimaryStorageMdsBase> it;
        private final List<MdsInfo> mdsInfos;
        private final ErrorCodeList errorCodes = new ErrorCodeList();

        private final String path;
        private final AgentCommand cmd;
        private final Class<T> retClass;
        private final ReturnValueCompletion<T> callback;
        private final TimeUnit unit;
        private final long timeout;
        private final boolean sync;

        private boolean tryNext = false;

        public HttpCaller(String path, AgentCommand cmd, Class<T> retClass, ReturnValueCompletion<T> callback) {
            this(path, cmd, retClass, callback, null, 0, false);
        }

        public HttpCaller(String path, AgentCommand cmd, Class<T> retClass, ReturnValueCompletion<T> callback, TimeUnit unit, long timeout, boolean sync) {
            this.path = path;
            this.cmd = cmd;
            this.retClass = retClass;
            this.callback = callback;
            this.unit = unit;
            this.timeout = timeout;
            this.mdsInfos = prepareMds();
            this.sync = sync;
        }

        public void call() {
            prepareMdsIterator();
            prepareCmd();
            doCall();
        }

        public T syncCall() {
            prepareMdsIterator();
            prepareCmd();
            return doSyncCall();
        }

        HttpCaller<T> setTargetMds(String mdsAddr) {
            logger.debug(String.format("target MDS[%s]", mdsAddr));

            mdsInfos.removeIf(it -> !it.getAddr().equals(mdsAddr));
            if (mdsInfos.isEmpty()) {
                throw new OperationFailureException(operr(
                        "not found MDS[%s] of zbs primary storage[uuid:%s] node", mdsAddr, self.getUuid())
                );
            }

            return this;
        }

        private void prepareCmd() {
            cmd.setUuid(self.getUuid());
        }

        private List<MdsInfo> prepareMds() {
            final List<MdsInfo> mds = new ArrayList<>(addonInfo.getMdsInfos());

            Collections.shuffle(mds);

            mds.removeIf(it -> it.getStatus() != MdsStatus.Connected);
            if (mds.isEmpty()) {
                throw new OperationFailureException(operr(
                        "all MDS of ZBS primary storage[uuid:%s] are not in Connected state", self.getUuid())
                );
            }

            return mds;
        }

        private void prepareMdsIterator() {
            it = mdsInfos.stream().map(ZbsPrimaryStorageMdsBase::new).collect(Collectors.toList()).iterator();
        }

        private T doSyncCall() {
            if (!it.hasNext()) {
                throw new OperationFailureException(multiErr(errorCodes, "all MDS cannot execute http call[%s]", path));
            }

            ZbsPrimaryStorageMdsBase base = it.next();
            cmd.setAddr(base.getSelf().getAddr());

            T ret = base.syncCall(path, cmd, retClass, unit, timeout);
            if (!ret.isSuccess()) {
                logger.warn(String.format("failed to execute http call[%s] on MDS[%s], error is: %s",
                        path, base.getSelf().getAddr(), JSONObjectUtil.toJsonString(ret.getError())));
                errorCodes.add(operr(ret.getError()));
                if (tryNext) {
                    return doSyncCall();
                } else {
                    throw new OperationFailureException(multiErr(errorCodes, "all MDS cannot execute http call[%s]", path));
                }
            }

            return ret;
        }

        private void doCall() {
            if (!it.hasNext()) {
                callback.fail(multiErr(errorCodes, "all MDS cannot execute http call[%s]", path));
                return;
            }

            ZbsPrimaryStorageMdsBase base = it.next();
            cmd.setAddr(base.getSelf().getAddr());

            ReturnValueCompletion<T> completion = new ReturnValueCompletion<T>(callback) {
                @Override
                public void success(T ret) {
                    callback.success(ret);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    if (!errorCode.isError(SysErrors.OPERATION_ERROR) && !errorCode.isError(SysErrors.TIMEOUT)) {
                        logger.warn(String.format("failed to execute http call[%s] on MDS[%s], error is: %s",
                                path, base.getSelf().getAddr(), JSONObjectUtil.toJsonString(errorCode)));
                        errorCodes.getCauses().add(errorCode);
                        doCall();
                        return;
                    }

                    if (tryNext) {
                        doCall();
                    } else {
                        callback.fail(errorCode);
                    }
                }
            };

            if (unit == null) {
                base.httpCall(path, cmd, retClass, completion);
            } else {
                base.httpCall(path, cmd, retClass, completion, unit, timeout);
            }
        }
    }

    public static class ExpandVolumeRsp extends AgentResponse {
        private long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class CopyRsp extends AgentResponse {
        private String installPath;
        private long size;
        private long actualSize;

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

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }
    }

    public static class RollbackSnapshotRsp extends AgentResponse {
        private String installPath;
        private long size;
        private long actualSize;

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

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }
    }

    public static class DeleteSnapshotRsp extends AgentResponse {

    }

    public static class QueryVolumeRsp extends AgentResponse {
        private long size;
        private long actualSize;
        private String parentUri;

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

        public String getParentUri() {
            return parentUri;
        }

        public void setParentUri(String parentUri) {
            this.parentUri = parentUri;
        }
    }

    public static class FlattenVolumeRsp extends QueryVolumeRsp {

    }

    public static class BatchQueryVolumeRsp extends AgentResponse {
        private Map<String, Map<String, Long>> volumes;

        public Map<String, Map<String, Long>> getVolumes() {
            return volumes;
        }

        public void setVolumes(Map<String, Map<String, Long>> volumes) {
            this.volumes = volumes;
        }
    }

    public static class CbdToNbdRsp extends AgentResponse {
        private String ip;
        private int port;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class CloneVolumeRsp extends AgentResponse {
        private long size;
        private long actualSize;
        private String installPath;

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

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class CreateSnapshotRsp extends AgentResponse {
        private String installPath;
        private long size;
        private long actualSize;

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

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }
    }

    public static class DeleteVolumeRsp extends AgentResponse {
    }

    public static class CreateVolumeRsp extends AgentResponse {
        private String installPath;
        private long size;
        private long actualSize;

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

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }
    }

    public static class GetCapacityRsp extends AgentResponse {
        private List<LogicalPoolInfo> logicalPoolInfos;

        public List<LogicalPoolInfo> getLogicalPoolInfos() {
            return logicalPoolInfos;
        }

        public void setLogicalPoolInfos(List<LogicalPoolInfo> logicalPoolInfos) {
            this.logicalPoolInfos = logicalPoolInfos;
        }
    }

    public static class DeployClientRsp extends AgentResponse {

    }

    public static class ExpandVolumeCmd extends SizeCmd {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class CopyCmd extends AgentCommand implements HasThreadContext {
        private String path;
        private String dstVolume;
        private long dstSize;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getDstVolume() {
            return dstVolume;
        }

        public void setDstVolume(String dstVolume) {
            this.dstVolume = dstVolume;
        }

        public long getDstSize() {
            return dstSize;
        }

        public void setDstSize(long dstSize) {
            this.dstSize = dstSize;
        }
    }

    public static class RollbackSnapshotCmd extends AgentCommand {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class DeleteSnapshotCmd extends AgentCommand {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class QueryVolumeCmd extends AgentCommand {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class FlattenVolumeCmd extends AgentCommand {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class BatchQueryVolumeCmd extends AgentCommand {
        private Collection<String> installPaths;

        public Collection<String> getInstallPaths() {
            return installPaths;
        }

        public void setInstallPaths(Collection<String> installPaths) {
            this.installPaths = installPaths;
        }
    }

    public static class CleanNbdCmd extends AgentCommand {
        private String nbdAddr;
        private int port;

        public String getNbdAddr() {
            return nbdAddr;
        }

        public void setNbdAddr(String nbdAddr) {
            this.nbdAddr = nbdAddr;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class CbdToNbdCmd extends AgentCommand {
        private String path;
        private String portRange;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getPortRange() {
            return portRange;
        }

        public void setPortRange(String portRange) {
            this.portRange = portRange;
        }
    }

    public static class CloneVolumeCmd extends AgentCommand {
        private String path;
        private String dstVolume;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getDstVolume() {
            return dstVolume;
        }

        public void setDstVolume(String dstVolume) {
            this.dstVolume = dstVolume;
        }
    }

    public static class CreateSnapshotCmd extends AgentCommand {
        private String path;
        private String snapshot;
        private boolean skipOnExisting;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getSnapshot() {
            return snapshot;
        }

        public void setSnapshot(String snapshot) {
            this.snapshot = snapshot;
        }

        public boolean isSkipOnExisting() {
            return skipOnExisting;
        }

        public void setSkipOnExisting(boolean skipOnExisting) {
            this.skipOnExisting = skipOnExisting;
        }
    }

    public static class DeleteVolumeCmd extends AgentCommand {
        private String path;
        private boolean force;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public boolean isForce() {
            return force;
        }

        public void setForce(boolean force) {
            this.force = force;
        }
    }

    public static class GetVolumeClientsCmd extends AgentCommand {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class ClientInfo {
        public ClientInfo(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        private String ip;
        private int port;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class GetVolumeClientsRsp extends AgentResponse {
        private List<ClientInfo> clients;

        public List<ClientInfo> getClients() {
            return clients;
        }

        public void setClients(List<ClientInfo> clients) {
            this.clients = clients;
        }
    }

    public static class SizeCmd extends AgentCommand {
        private long size;
        private String unit;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }

    public static class CreateVolumeCmd extends SizeCmd {
        private String logicalPool;
        private String volume;
        private boolean skipIfExisting;

        public String getLogicalPool() {
            return logicalPool;
        }

        public void setLogicalPool(String logicalPool) {
            this.logicalPool = logicalPool;
        }

        public String getVolume() {
            return volume;
        }

        public void setVolume(String volume) {
            this.volume = volume;
        }

        public boolean isSkipIfExisting() {
            return skipIfExisting;
        }

        public void setSkipIfExisting(boolean skipIfExisting) {
            this.skipIfExisting = skipIfExisting;
        }
    }

    public static class GetCapacityCmd extends AgentCommand {
        private String logicalPool;

        public String getLogicalPool() {
            return logicalPool;
        }

        public void setLogicalPool(String logicalPool) {
            this.logicalPool = logicalPool;
        }
    }

    public static class DeployClientCmd extends AgentCommand {
        private String ip;
        private Integer port;
        private String username;
        private String password;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class GetFactsRsp extends AgentResponse {
        private String uuid;
        private String version;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public static class GetFactsCmd extends AgentCommand {
    }

    public static class CheckHostStorageConnectionCmd extends AgentCommand {
        public String hostUuid;
        private String path;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class CheckHostStorageConnectionRsp extends AgentResponse {
    }

    public static class AgentResponse extends ZbsMdsBase.AgentResponse {
    }

    public static class AgentCommand extends ZbsMdsBase.AgentCommand {
    }

    public ZbsStorageController(ExternalPrimaryStorageVO self) {
        this.self = self;
        this.reloadDbInfo();
    }
}

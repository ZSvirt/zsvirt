package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.metadata.MetadataRegistrationPersistHelper;
import org.zstack.compute.vm.metadata.VmMetadataBuilderUtils;
import org.zstack.compute.vm.metadata.dirty.VmMetadataDirtyMarker;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.Component;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.*;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceTreeVO;
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceVO;
import org.zstack.header.vm.*;
import org.zstack.header.vm.metadata.*;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.kvm.KVMConstant;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.tag.TagManager;
import org.zstack.storage.snapshot.group.VolumeSnapshotGroupBase;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by MaJin on 2020/10/10.
 */
public class MevocoVmManagerImpl implements VmInstanceExtensionManager, BeforeVmInstanceStopExtensionPoint, Component, VmIpChangedExtensionPoint, ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MevocoVmManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VmMetadataDirtyMarker vmMetadataDirtyMarker;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private VmInstanceExtensionPointEmitter extEmitter;

    private List<String> createVmFromVolumeWorkFlowElements;
    private List<String> createVmFromSnapshotWorkFlowElements;
    private List<String> createVmFromSnapshotGroupWorkFlowElements;

    private FlowChainBuilder createVmFromVolumeFlowBuilder;
    private FlowChainBuilder createVmFromSnapshotFlowBuilder;
    private FlowChainBuilder createVmFromSnapshotGroupFlowBuilder;

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APICreateVmInstanceFromVolumeMsg) {
            handle((APICreateVmInstanceFromVolumeMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromVolumeSnapshotMsg) {
            handle((APICreateVmInstanceFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromVolumeSnapshotGroupMsg) {
            handle((APICreateVmInstanceFromVolumeSnapshotGroupMsg) msg);
        } else if (msg instanceof APIRegisterVmInstanceFromMetadataMsg) {
            handle((APIRegisterVmInstanceFromMetadataMsg) msg);
        } else if (msg instanceof UpdateVmInstanceMetadataMsg) {
            handle((UpdateVmInstanceMetadataMsg) msg);
        } else if (msg instanceof APICleanupVmInstanceMetadataMsg) {
            handle((APICleanupVmInstanceMetadataMsg) msg);
        } else if (msg instanceof APIUpdateVmInstanceMetadataMsg) {
            handle((APIUpdateVmInstanceMetadataMsg) msg);
        } else if (msg instanceof APIGetVmInstanceMetadataFromPrimaryStorageMsg) {
            handle((APIGetVmInstanceMetadataFromPrimaryStorageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public List<Class <? extends Message>> getMessageClasses() {
        return Arrays.asList(APICreateVmInstanceFromVolumeMsg.class,
                APICreateVmInstanceFromVolumeSnapshotMsg.class,
                APICreateVmInstanceFromVolumeSnapshotGroupMsg.class,
                APIRegisterVmInstanceFromMetadataMsg.class,
                APICleanupVmInstanceMetadataMsg.class,
                APIUpdateVmInstanceMetadataMsg.class,
                APIGetVmInstanceMetadataFromPrimaryStorageMsg.class,
                UpdateVmInstanceMetadataMsg.class
        );
    }

    @Override
    public boolean start() {
        pluginRgty.saveExtensionAsMap(VmMetadataPathBuildExtensionPoint.class, new Function<Object, VmMetadataPathBuildExtensionPoint>() {
            @Override
            public Object call(VmMetadataPathBuildExtensionPoint arg) {
                return arg.getPrimaryStorageType();
            }
        });
        pluginRgty.saveExtensionAsMap(VmMetadataPathReplacementExtensionPoint.class, new Function<Object, VmMetadataPathReplacementExtensionPoint>() {
            @Override
            public Object call(VmMetadataPathReplacementExtensionPoint arg) {
                return arg.getPrimaryStorageType();
            }
        });
        createVmFlowChainBuilder();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void createVmFlowChainBuilder() {
        createVmFromVolumeFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(createVmFromVolumeWorkFlowElements).construct();
        createVmFromSnapshotFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(createVmFromSnapshotWorkFlowElements).construct();
        createVmFromSnapshotGroupFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(createVmFromSnapshotGroupWorkFlowElements).construct();
    }

    public FlowChain getCreateVmFromVolumeWorkFlowChain() {
        return createVmFromVolumeFlowBuilder.build();
    }

    public FlowChain getCreateVmFromSnapshotWorkFlowChain() {
        return createVmFromSnapshotFlowBuilder.build();
    }

    public FlowChain getCreateVmFromSnapshotGroupWorkFlowChain() {
        return createVmFromSnapshotGroupFlowBuilder.build();
    }

    private void handle(final APICreateVmInstanceFromVolumeMsg msg) {
        APICreateVmInstanceFromVolumeEvent event = new APICreateVmInstanceFromVolumeEvent(msg.getId());
        CreateVmFromVolumeResourceSpec spec = new CreateVmFromVolumeResourceSpec();

        spec.setApiMsg(msg);
        spec.setPrimaryStorageUuidForRootVolume(msg.getPrimaryStorageUuid());
        spec.setOriginVolumeUuidForRootVolume(msg.getVolumeUuid());

        Map<String, Object> datas = new HashMap<>();
        datas.put(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC, spec);
        FlowChain chain = getCreateVmFromVolumeWorkFlowChain();
        chain.setName("create-vm-from-volume-" + msg.getVolumeUuid());
        chain.setData(datas);
        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                CreateVmFromVolumeResourceSpec spec = (CreateVmFromVolumeResourceSpec) data.get(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC);
                event.setInventory(spec.getVmInstance());
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

    private void handle(final APICreateVmInstanceFromVolumeSnapshotMsg msg) {
        APICreateVmInstanceFromVolumeSnapshotEvent event = new APICreateVmInstanceFromVolumeSnapshotEvent(msg.getId());
        CreateVmFromVolumeResourceSpec spec = new CreateVmFromVolumeResourceSpec();

        String volumeUuid = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.volumeUuid)
                .eq(VolumeSnapshotVO_.uuid, msg.getVolumeSnapshotUuid())
                .findValue();

        spec.setApiMsg(msg);
        spec.setPrimaryStorageUuidForRootVolume(msg.getPrimaryStorageUuid());
        spec.setOriginVolumeUuidForRootVolume(volumeUuid);
        spec.setRootVolumeSystemTags(msg.getRootVolumeSystemTags());

        Map<String, Object> datas = new HashMap<>();
        datas.put(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC, spec);
        FlowChain chain = getCreateVmFromSnapshotWorkFlowChain();
        chain.setName("create-vm-from-snapshot-" + msg.getVolumeSnapshotUuid());
        chain.setData(datas);
        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                CreateVmFromVolumeResourceSpec spec = (CreateVmFromVolumeResourceSpec) data.get(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC);
                event.setInventory(spec.getVmInstance());
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

    private void handle(final APICreateVmInstanceFromVolumeSnapshotGroupMsg msg) {
        APICreateVmInstanceFromVolumeSnapshotGroupEvent event = new APICreateVmInstanceFromVolumeSnapshotGroupEvent(msg.getId());
        CreateVmFromVolumeResourceSpec spec = new CreateVmFromVolumeResourceSpec();

        String rootVolumeUuid = SQL.New("select snapshot.volumeUuid from VolumeSnapshotGroupRefVO ref, VolumeSnapshotVO snapshot" +
                " where ref.volumeSnapshotGroupUuid = :groupUuid" +
                " and snapshot.uuid = ref.volumeSnapshotUuid" +
                " and snapshot.volumeType = :rootVolType", String.class)
                .param("groupUuid", msg.getVolumeSnapshotGroupUuid())
                .param("rootVolType", VolumeType.Root.toString())
                .find();

        VolumeSnapshotGroupVO group = Q.New(VolumeSnapshotGroupVO.class).eq(VolumeSnapshotGroupVO_.uuid, msg.getVolumeSnapshotGroupUuid()).find();
        List<VolumeSnapshotVO> snaps = new VolumeSnapshotGroupBase(group).getEffectiveSnapshots();
        snaps.removeIf(it -> !it.getVolumeType().equals(VolumeType.Data.toString()));

        spec.setApiMsg(msg);
        spec.setPrimaryStorageUuidForRootVolume(msg.getPrimaryStorageUuidForRootVolume());
        spec.setOriginVolumeUuidForRootVolume(rootVolumeUuid);
        spec.setRootVolumeSystemTags(msg.getRootVolumeSystemTags());

        Map<String, Object> datas = new HashMap<>();
        datas.put(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC, spec);
        datas.put(PremiumVmInstanceConstant.DATA_VOLUME_SNAPSHOT, snaps);
        FlowChain chain = getCreateVmFromSnapshotGroupWorkFlowChain();
        chain.setName("create-vm-from-snaphost-group-" + msg.getVolumeSnapshotGroupUuid());
        chain.setData(datas);
        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                CreateVmFromVolumeResourceSpec spec = (CreateVmFromVolumeResourceSpec) data.get(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC);
                event.setInventory(spec.getVmInstance());
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

    private static boolean isVolumeRecovering(String installPath) {
        try {
            final String query = new URI(installPath).getQuery();
            return query != null;
        } catch (URISyntaxException ignored) {
            return false;
        }
    }

    @Override
    public void beforeVmInstanceStop(VmInstanceInventory inv, Completion completion) {
        if (!KVMConstant.KVM_HYPERVISOR_TYPE.equals(inv.getHypervisorType())) {
            completion.success();
            return;
        }
        
        HostVO hostVO = dbf.findByUuid(inv.getHostUuid(), HostVO.class);
        if (hostVO.getStatus() == HostStatus.Disconnected) {
            completion.success();
            return;
        }

        Set<String> maySupportChainSnapshotPsUuids = new HashSet<>(Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.uuid)
                .notEq(PrimaryStorageVO_.type, CephConstants.CEPH_PRIMARY_STORAGE_TYPE)
                .listValues());
        boolean needCheck = inv.getAllDiskVolumes().stream()
                .anyMatch(it -> maySupportChainSnapshotPsUuids.contains(it.getPrimaryStorageUuid()));
        if (!needCheck) {
            completion.success();
            return;
        }

        if (inv.getAllDiskVolumes().stream().anyMatch(v -> isVolumeRecovering(v.getInstallPath()))) {
            logger.info(String.format("VM[uuid: %s] is recovering: skip volume check.", inv.getUuid()));
            completion.success();
            return;
        }

        CheckVmVolumesMsg msg = new CheckVmVolumesMsg();
        msg.setVmInstance(inv);
        msg.setHostUuid(inv.getHostUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    public void setCreateVmFromVolumeWorkFlowElements(List<String> createVmFromVolumeWorkFlowElements) {
        this.createVmFromVolumeWorkFlowElements = createVmFromVolumeWorkFlowElements;
    }

    public void setCreateVmFromSnapshotWorkFlowElements(List<String> createVmFromSnapshotWorkFlowElements) {
        this.createVmFromSnapshotWorkFlowElements = createVmFromSnapshotWorkFlowElements;
    }

    public void setCreateVmFromSnapshotGroupWorkFlowElements(List<String> createVmFromSnapshotGroupWorkFlowElements) {
        this.createVmFromSnapshotGroupWorkFlowElements = createVmFromSnapshotGroupWorkFlowElements;
    }

    public List<String> getCreateVmFromSnapshotWorkFlowElements() {
        return createVmFromSnapshotWorkFlowElements;
    }

    public List<String> getCreateVmFromSnapshotGroupWorkFlowElements() {
        return createVmFromSnapshotGroupWorkFlowElements;
    }

    public List<String> getCreateVmFromVolumeWorkFlowElements() {
        return createVmFromVolumeWorkFlowElements;
    }

    @Override
    public void vmIpChanged(VmInstanceInventory vm, VmNicInventory nic, Map<Integer, UsedIpInventory> oldIpMap, Map<Integer, UsedIpInventory> newIpMap) {
        String tagValue = VmSystemTags.CLEAN_TRAFFIC.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.CLEAN_TRAFFIC_TOKEN);
        if (!Boolean.parseBoolean(tagValue)) {
            return;
        }

        if (!vm.getState().equals(VmInstanceState.Running.toString())) {
            return;
        }

        VmUpdateNicOnHypervisorMsg cmsg = new VmUpdateNicOnHypervisorMsg();
        cmsg.setVmInstanceUuid(vm.getUuid());
        cmsg.setHostUuid(vm.getHostUuid());
        bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, vm.getUuid());
        bus.send(cmsg);
    }

    private void handle(APIRegisterVmInstanceFromMetadataMsg msg) {
        APIRegisterVmInstanceFromMetadataEvent event = new APIRegisterVmInstanceFromMetadataEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "register-vm-from-metadata-" + msg.getMetadataPath();
            }

            @Override
            public void run(SyncTaskChain chain) {
                doRegisterVmFromMetadata(msg, new ReturnValueCompletion<VmInstanceInventory>(msg, chain) {
                    @Override
                    public void success(VmInstanceInventory inv) {
                        event.setInventory(inv);
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "register-vm-from-metadata";
            }
        });
    }

    private void doRegisterVmFromMetadata(APIRegisterVmInstanceFromMetadataMsg msg, ReturnValueCompletion<VmInstanceInventory> completion) {
        String primaryStorageUuid = msg.getPrimaryStorageUuid();
        String zoneUuid = msg.getZoneUuid();
        String clusterUuid = msg.getClusterUuid();
        String hostUuid = msg.getHostUuid();
        String accountUuid = msg.getSession().getAccountUuid();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("register-vm-from-metadata-on-ps-" + primaryStorageUuid);
        chain.then(new ShareFlow() {
            String metadataJson;
            VmInstanceMetadataDTO dto;
            String vmUuid;
            Map<String, String> metadataToCurrentPathMap;
            String psType;
            VmMetadataPathReplacementExtensionPoint.PathReplacementResult replacement;
            MetadataRegistrationPersistHelper.ValidateResult validateResult;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "get-metadata-from-agent";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        GetVmInstanceMetadataFromPrimaryStorageMsg gmsg = new GetVmInstanceMetadataFromPrimaryStorageMsg();
                        gmsg.setPrimaryStorageUuid(primaryStorageUuid);
                        gmsg.setHostUuid(msg.getHostUuid());
                        gmsg.setMetadataPath(msg.getMetadataPath());
                        bus.makeTargetServiceIdByResourceUuid(gmsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
                        bus.send(gmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }
                                GetVmInstanceMetadataFromPrimaryStorageReply r = reply.castReply();
                                metadataJson = r.getMetadata();
                                if (StringUtils.isEmpty(metadataJson)) {
                                    trigger.fail(Platform.operr("agent returned empty metadata for " +
                                            "primaryStorage[uuid:%s] path[%s]", primaryStorageUuid, msg.getMetadataPath()));
                                    return;
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "parse-metadata";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        try {
                            dto = JSONObjectUtil.toObject(metadataJson, VmInstanceMetadataDTO.class);
                        } catch (Exception e) {
                            trigger.fail(Platform.operr("failed to parse metadata JSON: %s", e.getMessage()));
                            return;
                        }

                        if (dto == null || dto.getVm() == null) {
                            trigger.fail(Platform.operr("invalid metadata: DTO or VM section is null"));
                            return;
                        }

                        vmUuid = dto.getVm().getResourceUuid();
                        if (StringUtils.isEmpty(vmUuid)) {
                            trigger.fail(Platform.operr("vm resourceUuid is missing in metadata"));
                            return;
                        }

                        VmInstanceVO existingVm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
                        if (existingVm != null && existingVm.getState() != VmInstanceState.Registering) {
                            trigger.fail(Platform.operr("vm[uuid:%s] already exists in state[%s], refuse duplicate registration", vmUuid, existingVm.getState()));
                            return;
                        }

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "validate-metadata-content";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VmInstanceVO existingVm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
                        if (existingVm != null && existingVm.getState() == VmInstanceState.Registering) {
                            String ownerMnUuid = VmSystemTags.VM_METADATA_REGISTERING_MN_UUID
                                    .getTokenByResourceUuid(vmUuid, VmSystemTags.VM_METADATA_REGISTERING_MN_UUID_TOKEN);
                            boolean ownerAlive = ownerMnUuid != null
                                    && Q.New(ManagementNodeVO.class).eq(ManagementNodeVO_.uuid, ownerMnUuid).isExists();
                            if (ownerAlive) {
                                trigger.fail(Platform.operr("vm[uuid:%s] is being registered by management node[uuid:%s], " +
                                        "refuse concurrent registration", vmUuid, ownerMnUuid));
                                return;
                            }

                            logger.warn(String.format("[VmRegistration] found stale Registering vm[uuid:%s] " +
                                    "(ownerMn=%s, alive=false), rolling back before re-registration", vmUuid, ownerMnUuid));
                            MetadataRegistrationPersistHelper.rollbackRegistration(vmUuid);
                        }

                        validateResult = MetadataRegistrationPersistHelper.validateMetadataContent(dto);
                        if (validateResult.error != null) {
                            trigger.fail(validateResult.error);
                            return;
                        }

                        VmInstanceVO vmFromMetadata = JSONObjectUtil.toObject(dto.getVm().getVo(), VmInstanceVO.class);
                        String vmArch = vmFromMetadata.getArchitecture();
                        String clusterArch = Q.New(ClusterVO.class).select(ClusterVO_.architecture)
                                .eq(ClusterVO_.uuid, clusterUuid).findValue();
                        if (vmArch != null && clusterArch != null && !vmArch.equals(clusterArch)) {
                            trigger.fail(Platform.operr(
                                    "architecture mismatch: vm[uuid:%s] in metadata has architecture[%s], " +
                                    "but target cluster[uuid:%s] has architecture[%s]",
                                    vmUuid, vmArch, clusterUuid, clusterArch));
                            return;
                        }

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "calculate-paths";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        psType = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type)
                                .eq(PrimaryStorageVO_.uuid, primaryStorageUuid).findValue();

                        List<String> allOldPaths = new ArrayList<>();
                        if (dto.getVolumes() != null) {
                            for (VolumeResourceMetadata volMeta : dto.getVolumes()) {
                                VolumeVO vol = JSONObjectUtil.toObject(volMeta.getVo(), VolumeVO.class);
                                if (vol.getInstallPath() != null) allOldPaths.add(vol.getInstallPath());

                                if (volMeta.getSnapshotReferenceTree() != null) {
                                    VolumeSnapshotReferenceTreeVO t = JSONObjectUtil.toObject(volMeta.getSnapshotReferenceTree(), VolumeSnapshotReferenceTreeVO.class);
                                    if (t.getRootInstallUrl() != null) allOldPaths.add(t.getRootInstallUrl());
                                }
                                if (volMeta.getSnapshotReference() != null) {
                                    VolumeSnapshotReferenceVO r = JSONObjectUtil.toObject(volMeta.getSnapshotReference(), VolumeSnapshotReferenceVO.class);
                                    if (r.getVolumeSnapshotInstallUrl() != null)
                                        allOldPaths.add(r.getVolumeSnapshotInstallUrl());
                                    if (r.getDirectSnapshotInstallUrl() != null)
                                        allOldPaths.add(r.getDirectSnapshotInstallUrl());
                                    if (r.getReferenceInstallUrl() != null)
                                        allOldPaths.add(r.getReferenceInstallUrl());
                                }
                            }
                        }
                        if (dto.getSnapshots() != null) {
                            for (String snapJson : dto.getSnapshots()) {
                                VolumeSnapshotVO snap = JSONObjectUtil.toObject(snapJson, VolumeSnapshotVO.class);
                                if (snap.getPrimaryStorageInstallPath() != null)
                                    allOldPaths.add(snap.getPrimaryStorageInstallPath());
                            }
                        }
                        if (dto.getSnapshotGroupRefs() != null) {
                            for (String refJson : dto.getSnapshotGroupRefs()) {
                                VolumeSnapshotGroupRefVO ref = JSONObjectUtil.toObject(refJson, VolumeSnapshotGroupRefVO.class);
                                if (ref.getVolumeSnapshotInstallPath() != null)
                                    allOldPaths.add(ref.getVolumeSnapshotInstallPath());
                            }
                        }

                        metadataToCurrentPathMap = Collections.emptyMap();
                        replacement = null;
                        VmMetadataPathReplacementExtensionPoint ext = pluginRgty.getExtensionFromMap(psType, VmMetadataPathReplacementExtensionPoint.class);
                        if (ext != null) {
                            replacement = ext.calculatePathReplacements(primaryStorageUuid, allOldPaths);
                            metadataToCurrentPathMap = replacement.getMetadataToCurrentPathMap() != null ? replacement.getMetadataToCurrentPathMap() : Collections.emptyMap();
                        }

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "rebase-backing-file-prefixes";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (replacement == null || replacement.getOldPrefix() == null || replacement.getNewPrefix() == null || replacement.getOldPrefix().equals(replacement.getNewPrefix())) {
                            trigger.next();
                            return;
                        }

                        List<String> installPaths = new ArrayList<>();
                        if (dto.getVolumes() != null) {
                            for (VolumeResourceMetadata volMeta : dto.getVolumes()) {
                                VolumeVO vol = JSONObjectUtil.toObject(volMeta.getVo(), VolumeVO.class);
                                // memory volume/snapshot has no backing chain
                                if (vol.isMemoryVolume()) {
                                    continue;
                                }
                                String p = vol.getInstallPath();
                                if (p != null) {
                                    installPaths.add(metadataToCurrentPathMap.getOrDefault(p, p));
                                }
                            }
                        }
                        if (dto.getSnapshots() != null) {
                            for (String snapJson : dto.getSnapshots()) {
                                VolumeSnapshotVO snap = JSONObjectUtil.toObject(snapJson, VolumeSnapshotVO.class);
                                // memory volume/snapshot has no backing chain
                                if (VolumeType.Memory.toString().equals(snap.getVolumeType())) {
                                    continue;
                                }
                                String p = snap.getPrimaryStorageInstallPath();
                                if (p != null) {
                                    installPaths.add(metadataToCurrentPathMap.getOrDefault(p, p));
                                }
                            }
                        }

                        if (installPaths.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        RebaseVolumeBackingFileOnPrimaryStorageMsg rmsg = new RebaseVolumeBackingFileOnPrimaryStorageMsg();
                        rmsg.setPrimaryStorageUuid(primaryStorageUuid);
                        rmsg.setInstallPaths(installPaths);
                        rmsg.setOldPrefix(replacement.getOldPrefix());
                        rmsg.setNewPrefix(replacement.getNewPrefix());
                        rmsg.setHostUuid(hostUuid);
                        bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
                        bus.send(rmsg, new CloudBusCallBack(trigger) {
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

                flow(new Flow() {
                    String __name__ = "persist-vm-and-resources";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        try {
                            MetadataRegistrationPersistHelper.PersistContext ctx =
                                    new MetadataRegistrationPersistHelper.PersistContext(dto, vmUuid, primaryStorageUuid,
                                            psType, metadataToCurrentPathMap, zoneUuid, clusterUuid, hostUuid, accountUuid, msg.getName(),
                                            validateResult.parsedTagsAndConfigsMap);
                            MetadataRegistrationPersistHelper.persistVmAndResources(ctx);
                        } catch (Exception e) {
                            trigger.fail(Platform.operr("persist failed for vm[uuid:%s]: %s", vmUuid, e.getMessage()));
                            return;
                        }
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (vmUuid != null) {
                            MetadataRegistrationPersistHelper.rollbackRegistration(vmUuid);
                        }
                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "create-api-system-tags";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        try {
                            tagMgr.createTags(msg.getSystemTags(), msg.getUserTags(), vmUuid, VmInstanceVO.class.getSimpleName());
                        } catch (Exception e) {
                            trigger.fail(Platform.operr(
                                    "failed to persist api tags for vm[uuid:%s]: %s", vmUuid, e.getMessage()));
                            return;
                        }
                        if (msg.getSystemTags() != null && !msg.getSystemTags().isEmpty()) {
                            List<ErrorCode> errors = extEmitter.handleSystemTag(vmUuid, msg.getSystemTags());
                            if (!errors.isEmpty()) {
                                trigger.fail(Platform.operr(
                                        "failed to handle system tags for vm[uuid:%s]: %s", vmUuid, errors));
                                return;
                            }
                        }
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (vmUuid != null) {
                            MetadataRegistrationPersistHelper.rollbackRegistration(vmUuid);
                        }
                        trigger.rollback();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        VmInstanceVO resultVm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
                        if (resultVm == null) {
                            completion.fail(Platform.operr(
                                    "vm[uuid:%s] disappeared after persist (concurrent deletion?)", vmUuid));
                            return;
                        }

                        if (resultVm.getHostUuid() == null && resultVm.getLastHostUuid() == null) {
                            // Prefer API-supplied hostUuid (carries storage affinity for local storage),
                            // fall back to any connected host in the cluster.
                            String fallbackHostUuid = hostUuid;
                            if (fallbackHostUuid == null) {
                                fallbackHostUuid = Q.New(HostVO.class)
                                        .select(HostVO_.uuid)
                                        .eq(HostVO_.clusterUuid, clusterUuid)
                                        .eq(HostVO_.state, HostState.Enabled)
                                        .eq(HostVO_.status, HostStatus.Connected)
                                        .limit(1)
                                        .findValue();
                            }
                            if (fallbackHostUuid != null) {
                                resultVm.setLastHostUuid(fallbackHostUuid);
                                dbf.update(resultVm);
                            }
                        }

                        completion.success(VmInstanceInventory.valueOf(resultVm));
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(UpdateVmInstanceMetadataMsg msg) {
        UpdateVmInstanceMetadataReply reply = new UpdateVmInstanceMetadataReply();
        String vmUuid = msg.getVmInstanceUuid();

        String metadata = VmMetadataBuilderUtils.buildVmInstanceMetadata(dbf, vmUuid);
        if (metadata == null) {
            reply.setError(Platform.operr("metadata build returned null for vm[uuid=%s]", vmUuid));
            bus.reply(msg, reply);
            return;
        }

        Tuple tuple = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.name, VmInstanceVO_.architecture, VmInstanceVO_.rootVolumeUuid).findTuple();
        if (tuple == null) {
            reply.setError(Platform.operr("vm[uuid:%s] not found, possibly deleted during flush", vmUuid));
            bus.reply(msg, reply);
            return;
        }
        String vmName = tuple.get(0, String.class);
        String architecture = tuple.get(1, String.class);
        String rootVolumeUuid = tuple.get(2, String.class);

        if (rootVolumeUuid == null) {
            reply.setError(Platform.operr("vm[uuid:%s] has no root volume, skip metadata flush", vmUuid));
            bus.reply(msg, reply);
            return;
        }

        String primaryStorageUuid = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, rootVolumeUuid).select(VolumeVO_.primaryStorageUuid).findValue();
        if (primaryStorageUuid == null) {
            reply.setError(Platform.operr("root volume[uuid:%s] of vm[uuid:%s] has no primary storage, skip metadata flush",
                    rootVolumeUuid, vmUuid));
            bus.reply(msg, reply);
            return;
        }

        String psType = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type).eq(PrimaryStorageVO_.uuid, primaryStorageUuid).findValue();
        VmMetadataPathBuildExtensionPoint ext = pluginRgty.getExtensionFromMap(psType, VmMetadataPathBuildExtensionPoint.class);
        if (ext == null) {
            reply.setError(Platform.operr("primary storage type %s does not support metadata", psType));
            bus.reply(msg, reply);
            return;
        }
        String metadataPath = ext.buildVmMetadataPath(primaryStorageUuid, vmUuid);

        UpdateVmInstanceMetadataOnPrimaryStorageMsg umsg = new UpdateVmInstanceMetadataOnPrimaryStorageMsg();
        umsg.setVmInstanceUuid(vmUuid);
        umsg.setRootVolumeUuid(rootVolumeUuid);
        umsg.setVmInstanceName(vmName);
        umsg.setVmCategory(VmMetadataBuilderUtils.determineVmCategory(vmUuid).name());
        umsg.setArchitecture(architecture);
        umsg.setMetadata(metadata);
        umsg.setSchemaVersion(dbf.getDbVersion());
        umsg.setPrimaryStorageUuid(primaryStorageUuid);
        umsg.setStorageStructureChange(msg.isStorageStructureChange());
        umsg.setMetadataPath(metadataPath);
        bus.makeTargetServiceIdByResourceUuid(umsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        bus.send(umsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    reply.setError(Platform.operr("failed to update vm[uuid=%s] metadata on primary storage",
                            vmUuid).withCause(r.getError()));
                } else {
                    reply.setMetadata(metadata);
                }
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APICleanupVmInstanceMetadataMsg msg) {
        APICleanupVmInstanceMetadataEvent evt = new APICleanupVmInstanceMetadataEvent(msg.getId());

        List<String> vmUuids = msg.getVmUuids();
        List<CleanupVmInstanceMetadataOnPrimaryStorageMsg> cleanupMsgs = new ArrayList<>();
        List<String> skippedVmUuids = new ArrayList<>();
        for (String vmUuid : vmUuids) {
            Tuple t = Q.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, vmUuid).eq(VolumeVO_.type, VolumeType.Root)
                    .select(VolumeVO_.uuid, VolumeVO_.primaryStorageUuid).findTuple();
            if (t == null) {
                logger.warn(String.format("[MetadataCleanup] skipped vm[uuid:%s]: no root volume found", vmUuid));
                skippedVmUuids.add(vmUuid);
                continue;
            }
            String rootVolumeUuid = t.get(0, String.class);
            String psUuid = t.get(1, String.class);

            String psType = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type).eq(PrimaryStorageVO_.uuid, psUuid).findValue();
            if (psType == null) {
                logger.warn(String.format("[MetadataCleanup] skipped vm[uuid:%s]: primary storage[uuid:%s] no longer exists", vmUuid, psUuid));
                skippedVmUuids.add(vmUuid);
                continue;
            }
            VmMetadataPathBuildExtensionPoint ext = pluginRgty.getExtensionFromMap(psType, VmMetadataPathBuildExtensionPoint.class);
            if (ext == null) {
                logger.warn(String.format("[MetadataCleanup] skipped vm[uuid:%s]: no metadata extension for ps type[%s]", vmUuid, psType));
                skippedVmUuids.add(vmUuid);
                continue;
            }
            String metadataPath = ext.buildVmMetadataPath(psUuid, vmUuid);

            CleanupVmInstanceMetadataOnPrimaryStorageMsg cmsg = new CleanupVmInstanceMetadataOnPrimaryStorageMsg();
            cmsg.setPrimaryStorageUuid(psUuid);
            cmsg.setVmInstanceUuid(vmUuid);
            cmsg.setRootVolumeUuid(rootVolumeUuid);
            cmsg.setMetadataPath(metadataPath);

            Tuple hostTuple = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid)
                    .eq(VmInstanceVO_.uuid, vmUuid).findTuple();
            if (hostTuple != null) {
                String vmHostUuid = hostTuple.get(0, String.class);
                if (vmHostUuid == null) {
                    vmHostUuid = hostTuple.get(1, String.class);
                }
                cmsg.setHostUuid(vmHostUuid);
            }

            bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, psUuid);
            cleanupMsgs.add(cmsg);
        }

        if (cleanupMsgs.isEmpty()) {
            evt.setTotalCleaned(0);
            evt.setTotalFailed(skippedVmUuids.size());
            evt.setFailedVmUuids(skippedVmUuids);
            bus.publish(evt);
            return;
        }

        List<String> failedVmUuids = Collections.synchronizedList(new ArrayList<>(skippedVmUuids));
        new While<>(cleanupMsgs).each((cmsg, whileCompletion) -> {
            bus.send(cmsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        failedVmUuids.add(cmsg.getVmInstanceUuid());
                        submitMetadataCleanupGC(cmsg);
                    }
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                int cleanupFailed = failedVmUuids.size() - skippedVmUuids.size();
                evt.setTotalCleaned(cleanupMsgs.size() - cleanupFailed);
                evt.setTotalFailed(failedVmUuids.size());
                evt.setFailedVmUuids(failedVmUuids);
                bus.publish(evt);
            }
        });
    }

    private void submitMetadataCleanupGC(CleanupVmInstanceMetadataOnPrimaryStorageMsg cmsg) {
        CleanupVmInstanceMetadataOnPrimaryStorageGC gc = new CleanupVmInstanceMetadataOnPrimaryStorageGC();
        gc.NAME = CleanupVmInstanceMetadataOnPrimaryStorageGC.getGCName(cmsg.getVmInstanceUuid());
        gc.primaryStorageUuid = cmsg.getPrimaryStorageUuid();
        gc.vmUuid = cmsg.getVmInstanceUuid();
        gc.rootVolumeUuid = cmsg.getRootVolumeUuid();
        gc.metadataPath = cmsg.getMetadataPath();
        gc.hostUuid = cmsg.getHostUuid();
        long gcIntervalSec = TimeUnit.HOURS.toSeconds(VmGlobalConfig.VM_METADATA_CLEANUP_GC_INTERVAL.value(Long.class));
        gc.deduplicateSubmit(gcIntervalSec, TimeUnit.SECONDS);

        logger.info(String.format("[MetadataCleanup] submitted GC job [%s] for vm[uuid:%s] on ps[uuid:%s]",
                gc.NAME, cmsg.getVmInstanceUuid(), cmsg.getPrimaryStorageUuid()));
    }

    private void handle(APIUpdateVmInstanceMetadataMsg msg) {
        APIUpdateVmInstanceMetadataEvent event = new APIUpdateVmInstanceMetadataEvent(msg.getId());
        if (vmMetadataDirtyMarker != null) {
            for (String vmUuid : msg.getVmUuids()) {
                vmMetadataDirtyMarker.markDirty(vmUuid, true);
            }
        }
        bus.publish(event);
    }

    private void handle(APIGetVmInstanceMetadataFromPrimaryStorageMsg msg) {
        APIGetVmInstanceMetadataFromPrimaryStorageEvent evt = new APIGetVmInstanceMetadataFromPrimaryStorageEvent(msg.getId());

        String vmInstanceUuid = msg.getVmInstanceUuid();
        Tuple t = Q.New(VolumeVO.class)
                .eq(VolumeVO_.vmInstanceUuid, vmInstanceUuid)
                .eq(VolumeVO_.type, VolumeType.Root)
                .select(VolumeVO_.uuid, VolumeVO_.primaryStorageUuid)
                .findTuple();
        if (t == null) {
            evt.setError(Platform.operr("cannot find root volume for vm[uuid:%s]", vmInstanceUuid));
            bus.publish(evt);
            return;
        }
        String rootVolumeUuid = t.get(0, String.class);
        String psUuid = t.get(1, String.class);

        String psType = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type).eq(PrimaryStorageVO_.uuid, psUuid).findValue();
        VmMetadataPathBuildExtensionPoint ext = pluginRgty.getExtensionFromMap(psType, VmMetadataPathBuildExtensionPoint.class);
        if (ext == null) {
            evt.setError(Platform.operr("primary storage type %s does not support metadata", psType));
            bus.publish(evt);
            return;
        }
        String metadataPath = ext.buildVmMetadataPath(psUuid, vmInstanceUuid);

        GetVmInstanceMetadataFromPrimaryStorageMsg gmsg = new GetVmInstanceMetadataFromPrimaryStorageMsg();
        gmsg.setPrimaryStorageUuid(psUuid);
        gmsg.setRootVolumeUuid(rootVolumeUuid);
        gmsg.setMetadataPath(metadataPath);
        bus.makeTargetServiceIdByResourceUuid(gmsg, PrimaryStorageConstant.SERVICE_ID, psUuid);
        bus.send(gmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    evt.setError(r.getError());
                    bus.publish(evt);
                    return;
                }
                GetVmInstanceMetadataFromPrimaryStorageReply re = r.castReply();
                evt.setMetadata(re.getMetadata());
                bus.publish(evt);
            }
        });
    }

    @Override
    public void managementNodeReady() {
        cleanupStaleRegisteringVms();
    }

    private void cleanupStaleRegisteringVms() {
        List<String> registeringVmUuids = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .eq(VmInstanceVO_.state, VmInstanceState.Registering)
                .listValues();

        if (registeringVmUuids.isEmpty()) {
            return;
        }

        // Collect all alive MN UUIDs once
        Set<String> aliveMnUuids = new HashSet<>(
                Q.New(ManagementNodeVO.class).select(ManagementNodeVO_.uuid).listValues());

        for (String vmUuid : registeringVmUuids) {
            String ownerMnUuid = VmSystemTags.VM_METADATA_REGISTERING_MN_UUID
                    .getTokenByResourceUuid(vmUuid, VmSystemTags.VM_METADATA_REGISTERING_MN_UUID_TOKEN);

            if (ownerMnUuid != null && aliveMnUuids.contains(ownerMnUuid)) {
                logger.info(String.format(
                        "[VmRegistration] Registering vm[uuid:%s] owned by alive MN[uuid:%s], skip rollback",
                        vmUuid, ownerMnUuid));
                continue;
            }

            logger.warn(String.format(
                    "[VmRegistration] found stale Registering vm[uuid:%s] (ownerMn=%s, alive=%s), rolling back",
                    vmUuid, ownerMnUuid, ownerMnUuid != null ? "false" : "tag missing"));
            try {
                MetadataRegistrationPersistHelper.rollbackRegistration(vmUuid);
            } catch (Exception e) {
                logger.warn(String.format("[VmRegistration] failed to rollback stale Registering vm[uuid:%s], continue", vmUuid), e);
            }
        }
    }
}

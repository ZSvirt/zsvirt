package org.zstack.pciDevice;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.compute.host.PostHostConnectExtensionPoint;
import org.zstack.compute.vm.VmCapabilitiesExtensionPoint;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.ha.HaHostDeviceExtensionPoint;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.identity.*;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.image.ImageBootMode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.sriov.EthernetVfPciDeviceVO;
import org.zstack.header.sriov.EthernetVfPciDeviceVO_;
import org.zstack.header.sriov.VmVfNicVO;
import org.zstack.header.sriov.VmVfNicVO_;
import org.zstack.header.vdpa.VmVdpaNicVO;
import org.zstack.header.vdpa.VmVdpaNicVO_;
import org.zstack.header.vm.*;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.kvm.*;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkCandidateFilterExtensionPoint;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.pciDevice.gpu.GpuDeviceVO;
import org.zstack.pciDevice.gpu.GpuDeviceVO_;
import org.zstack.pciDevice.quota.GPUNumQuotaDefinition;
import org.zstack.pciDevice.quota.GenericPCIDeviceNumQuotaDefinition;
import org.zstack.pciDevice.specification.mdev.APIAddMdevDeviceSpecToVmInstanceMsg;
import org.zstack.pciDevice.specification.mdev.MdevDeviceSpecVO;
import org.zstack.pciDevice.specification.mdev.MdevDeviceSpecVO_;
import org.zstack.pciDevice.specification.pci.*;
import org.zstack.pciDevice.virtual.*;
import org.zstack.pciDevice.virtual.vfio_mdev.APIAttachMdevDeviceToVmMsg;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceType;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceVO;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceVO_;
import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVmMsg;
import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVolumeMsg;
import org.zstack.storage.primary.local.APILocalStorageMigrateVolumeMsg;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by weiwang on 10/07/2017.
 */
public class PciDeviceManager extends AbstractService implements
        PciDeviceManagerInterface,
        ReportQuotaExtensionPoint,
        HaHostDeviceExtensionPoint,
        GlobalApiMessageInterceptor,
        VmCapabilitiesExtensionPoint,
        PostHostConnectExtensionPoint,
        KVMStartVmAddonExtensionPoint,
        VmInstanceMigrateExtensionPoint,
        VmReleaseResourceExtensionPoint,
        VmAbnormalLifeCycleExtensionPoint,
        AddtionalResourceTypeExtensionPoint,
        PreVmInstantiateResourceExtensionPoint,
        PostVmInstantiateResourceExtensionPoint,
        KVMHostConnectExtensionPoint,
        KvmReportVmShutdownEventExtensionPoint,
        HostNetworkCandidateFilterExtensionPoint {
    private static final CLogger logger = Utils.getLogger(PciDeviceManager.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected EventFacade evtf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private EventFacade evf;
    @Autowired
    private ThreadFacade thdf;

    private final Map<HypervisorType, PciDeviceBackend> backends = new HashMap<>();
    private final Map<String, VirtualPciDeviceFactory> virtualFactories = new HashMap<>();
    private final Map<PciDeviceType, PciDeviceTypeFactory> pciDeviceTypeFactories = new HashMap<>();

    private void populateExtensions() {
        for (PciDeviceBackend bkd : pluginRgty.getExtensionList(PciDeviceBackend.class)) {
            PciDeviceBackend old = backends.get(bkd.getHypervisorType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate PciDeviceBackend[%s, %s] for type[%s]",
                        old.getClass(), bkd.getClass(), bkd.getHypervisorType()));
            }

            backends.put(bkd.getHypervisorType(), bkd);
        }

        for (PciDeviceTypeFactory factory : pluginRgty.getExtensionList(PciDeviceTypeFactory.class)) {
            List<PciDeviceType> types = factory.getTypes();
            for (PciDeviceType type : types) {
                PciDeviceTypeFactory old = pciDeviceTypeFactories.get(type);
                if (old != null) {
                    throw new CloudRuntimeException(String.format("duplicate PciDeviceTypeFactory[%s, %s] for type[%s]",
                            old.getClass(), factory.getClass(), type));
                }
                pciDeviceTypeFactories.put(type, factory);
            }
        }

        pluginRgty.getExtensionList(VirtualPciDeviceFactory.class).forEach(f -> {
            VirtualPciDeviceFactory old = virtualFactories.get(f.getVirtTechType());
            if (old != null) {
                throw new CloudRuntimeException(String.format(
                        "duplicate VirtualPciDeviceFactory[%s, %s] with the same type[%s]",
                        old.getClass(), f.getClass(), f.getVirtTechType()
                ));
            }

            virtualFactories.put(f.getVirtTechType(), f);
        });
    }

    @Transactional
    @ExceptionSafe
    private void syncPciDeviceOfferingRef() {
        if (!PciGlobalProperty.SYNC_PCI_DEVICE_OFFERING_REF) {
            return;
        }

        List<PciDeviceOfferingVO> offerings = Q.New(PciDeviceOfferingVO.class).list();
        List<PciDeviceVO> deviceVOS = Q.New(PciDeviceVO.class).list();

        for (PciDeviceVO pci : deviceVOS) {
            List<String> matchedOfferingUuids = offerings.stream()
                    .filter(offering -> offering.matchPciDevice(pci))
                    .map(PciDeviceOfferingVO::getUuid)
                    .collect(Collectors.toList());

            matchedOfferingUuids.forEach(offeringUuid -> {
                if (!Q.New(PciDevicePciDeviceOfferingRefVO.class)
                        .eq(PciDevicePciDeviceOfferingRefVO_.pciDeviceOfferingUuid, offeringUuid)
                        .eq(PciDevicePciDeviceOfferingRefVO_.pciDeviceUuid, pci.getUuid()).isExists()) {
                    PciDevicePciDeviceOfferingRefVO refVO = new PciDevicePciDeviceOfferingRefVO();
                    refVO.setPciDeviceOfferingUuid(offeringUuid);
                    refVO.setPciDeviceUuid(pci.getUuid());
                    dbf.persist(refVO);
                }
            });
        }
    }

    private PciDeviceBackend getPciDeviceBackendByVmUuid(String vmUuid) {
        VmInstanceVO vmvo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).find();
        if (vmvo == null) {
            return null;
        }

        return backends.get(new HypervisorType(vmvo.getHypervisorType()));
    }

    @Override
    public PciDeviceBackend getPciDeviceBackendByHostUuid(String hostUuid) {
        HostVO hvo = Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).find();
        return backends.get(HypervisorType.valueOf(hvo.getHypervisorType()));
    }

    private VirtualPciDeviceFactory getVirtualPciDeviceFactory(String virtTechType) {
        VirtualPciDeviceFactory f = virtualFactories.get(virtTechType);
        if (f == null) {
            throw new CloudRuntimeException("cannot find VirtualPciDeviceFactory with type " + virtTechType);
        }
        return f;
    }

    @Override
    public PciDeviceTypeFactory getPciDeviceTypeFactory(PciDeviceType type) {
        PciDeviceTypeFactory f = pciDeviceTypeFactories.get(type);
        if (f == null) {
            return pciDeviceTypeFactories.get(PciDeviceType.Generic);
        }
        return f;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage(msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof SyncPciDeviceMsg) {
            handle((SyncPciDeviceMsg) msg);
        } else if (msg instanceof AttachPciDeviceToVmMsg) {
            handle((AttachPciDeviceToVmMsg) msg);
        } else if (msg instanceof DetachPciDeviceFromVmMsg) {
            handle((DetachPciDeviceFromVmMsg) msg);
        } else if (msg instanceof AttachPciDeviceToHostMsg) {
            handle((AttachPciDeviceToHostMsg) msg);
        } else if (msg instanceof DetachPciDeviceFromHostMsg) {
            handle((DetachPciDeviceFromHostMsg) msg);
        } else if (msg instanceof CheckAndReservePciDeviceMsg) {
            handle((CheckAndReservePciDeviceMsg) msg);
        } else if (msg instanceof CheckAndReservePciDeviceBySpecMsg) {
            handle((CheckAndReservePciDeviceBySpecMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(Message msg) {
        if (msg instanceof APIAttachPciDeviceToVmMsg) {
            handle((APIAttachPciDeviceToVmMsg) msg);
        } else if (msg instanceof APIDetachPciDeviceFromVmMsg) {
            handle((APIDetachPciDeviceFromVmMsg) msg);
        } else if (msg instanceof APICreatePciDeviceOfferingMsg) {
            handle((APICreatePciDeviceOfferingMsg) msg);
        } else if (msg instanceof APIDeletePciDeviceOfferingMsg) {
            handle((APIDeletePciDeviceOfferingMsg) msg);
        } else if (msg instanceof APIDeletePciDeviceMsg) {
            handle((APIDeletePciDeviceMsg) msg);
        } else if (msg instanceof APIUpdatePciDeviceMsg) {
            handle((APIUpdatePciDeviceMsg) msg);
        } else if (msg instanceof APIGetHostIommuStatusMsg) {
            handle((APIGetHostIommuStatusMsg) msg);
        } else if (msg instanceof APIGetHostIommuStateMsg) {
            handle((APIGetHostIommuStateMsg) msg);
        } else if (msg instanceof APIUpdateHostIommuStateMsg) {
            handle((APIUpdateHostIommuStateMsg) msg);
        } else if (msg instanceof APIGetPciDeviceCandidatesForAttachingVmMsg) {
            handle((APIGetPciDeviceCandidatesForAttachingVmMsg) msg);
        } else if (msg instanceof APIGetPciDeviceCandidatesForNewCreateVmMsg) {
            handle((APIGetPciDeviceCandidatesForNewCreateVmMsg) msg);
        } else if (msg instanceof APIGenerateVirtualPciDevicesMsg) {
            handle((APIGenerateVirtualPciDevicesMsg) msg);
        } else if (msg instanceof APIUngenerateVirtualPciDevicesMsg) {
            handle((APIUngenerateVirtualPciDevicesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGenerateVirtualPciDevicesMsg msg) {
        APIGenerateVirtualPciDevicesEvent evt = new APIGenerateVirtualPciDevicesEvent(msg.getId());
        VirtualPciDeviceFactory f = getVirtualPciDeviceFactory(msg.getVirtTechType());
        f.generateVirtualPciDevices(msg, new Completion(evt) {
            @Override
            public void success() {
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIUngenerateVirtualPciDevicesMsg msg) {
        APIUngenerateVirtualPciDevicesEvent evt = new APIUngenerateVirtualPciDevicesEvent(msg.getId());
        VirtualPciDeviceFactory f = getVirtualPciDeviceFactory(msg.getVirtTechType());
        f.ungenerateVirtualPciDevices(msg, new Completion(evt) {
            @Override
            public void success() {
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    @Transactional
    private void handle(APIGetPciDeviceCandidatesForNewCreateVmMsg msg) {
        final APIGetPciDeviceCandidatesForNewCreateVmReply reply = new APIGetPciDeviceCandidatesForNewCreateVmReply();
        List<String> hostUuids = new ArrayList<>();
        List<String> clusterUuids = new ArrayList<>();
        HostIommuGetter hostIommuGetter = new HostIommuGetter();
        List<PciDeviceType> types = null;
        if (msg.getTypes() != null) {
            types = msg.getTypes().stream()
                    .map(PciDeviceType::valueOf).collect(Collectors.toList());
        }

        if (msg.getHostUuid() == null) {
            if (msg.getClusterUuids() == null || msg.getClusterUuids().isEmpty()) {
                clusterUuids = Q.New(ClusterVO.class)
                        .select(ClusterVO_.uuid)
                        .eq(ClusterVO_.state, ClusterState.Enabled)
                        .listValues();
            } else {
                clusterUuids = Q.New(ClusterVO.class)
                        .select(ClusterVO_.uuid)
                        .in(ClusterVO_.uuid, msg.getClusterUuids())
                        .eq(ClusterVO_.state, ClusterState.Enabled)
                        .listValues();
            }

            hostUuids = Q.New(HostVO.class).select(HostVO_.uuid).in(HostVO_.clusterUuid, clusterUuids).listValues();
            hostUuids = hostUuids.stream()
                    .filter(h -> HostIommuStatusType.Active.equals(hostIommuGetter.getStatus(h)))
                    .filter(h -> HostIommuStateType.Enabled.equals(hostIommuGetter.getState(h)))
                    .collect(Collectors.toList());
        } else {
            hostUuids.add(msg.getHostUuid());
        }

        if (hostUuids.isEmpty()) {
            logger.debug(String.format("specified host[uuid: %s] is not qualified(iommu state enabled and iommu status active)" +
                            " or can not find qualified host in cluster[uuids: %s] or no qualified in zone, return empty",
                    msg.getHostUuid(), msg.getClusterUuids())
            );
            reply.setInventories(asList());
            bus.reply(msg, reply);
            return;
        }

        List<PciDeviceVO> pciDeviceVOS = getAttachablePciDevicesForVm(null, hostUuids, types);

        reply.setInventories(getAccountCanAccessPciDevices(PciDeviceInventory.valueOf(pciDeviceVOS), msg.getSession()));

        bus.reply(msg, reply);
        return;
    }

    private List<PciDeviceInventory> getAccountCanAccessPciDevices(List<PciDeviceInventory> pciDeviceInventories, SessionInventory session) {
        if (pciDeviceInventories == null || pciDeviceInventories.isEmpty()) {
            return pciDeviceInventories;
        } else if (acntMgr.isAdmin(session)) {
            return pciDeviceInventories;
        }

        final String accountUuid = session.getAccountUuid();
        List<String> canAccessPciDeviceUuids = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, PciDeviceVO.class);
        if (canAccessPciDeviceUuids == null || canAccessPciDeviceUuids.isEmpty()) {
            return new ArrayList<>();
        }

        return pciDeviceInventories.stream().filter(i -> canAccessPciDeviceUuids.contains(i.getUuid())).collect(Collectors.toList());
    }

    @Transactional
    private void handle(APIGetPciDeviceCandidatesForAttachingVmMsg msg) {
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        final APIGetPciDeviceCandidatesForAttachingVmReply reply = new APIGetPciDeviceCandidatesForAttachingVmReply();
        List<PciDeviceType> types = null;
        if (msg.getTypes() != null) {
            types = msg.getTypes().stream()
                    .map(PciDeviceType::valueOf).collect(Collectors.toList());
        }
        List<String> pciSpecUuids = msg.getPciSpecUuids();
        if (CollectionUtils.isEmpty(pciSpecUuids)) {
            pciSpecUuids = Q.New(VmInstancePciDeviceSpecRefVO.class)
                    .eq(VmInstancePciDeviceSpecRefVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                    .select(VmInstancePciDeviceSpecRefVO_.pciSpecUuid)
                    .listValues();
        }

        if (vmInstanceVO.getState().equals(VmInstanceState.Running) ) {
            if (CollectionUtils.isEmpty(pciSpecUuids)) {
                HostIommuGetter hostIommuGetter = new HostIommuGetter();
                if (HostIommuStatusType.Inactive.equals(
                        hostIommuGetter.getStatus(vmInstanceVO.getHostUuid()))
                        || HostIommuStateType.Disabled.equals(
                        hostIommuGetter.getState(vmInstanceVO.getHostUuid()))) {
                    reply.setInventories(asList());
                    bus.reply(msg, reply);
                    return;
                }
                List<PciDeviceVO> pciDeviceVOS = getAttachablePciDevicesForVm(vmInstanceVO.getUuid(), asList(vmInstanceVO.getHostUuid()), types);
                reply.setInventories(getAccountCanAccessPciDevices(PciDeviceInventory.valueOf(pciDeviceVOS), msg.getSession()));
                bus.reply(msg, reply);
                return;
            }

            List<PciDeviceVO> pciDeviceVOS = getAttachablePciDevicesForVmBindPciSpec(vmInstanceVO.getUuid(), asList(vmInstanceVO.getHostUuid()), types, pciSpecUuids);
            reply.setInventories(getAccountCanAccessPciDevices(PciDeviceInventory.valueOf(pciDeviceVOS), msg.getSession()));
            bus.reply(msg, reply);
        } else if (vmInstanceVO.getState().equals(VmInstanceState.Stopped)) {
            getPciDeviceCandidatesForStoppedVm(vmInstanceVO.getUuid(), types, new ReturnValueCompletion<List<PciDeviceInventory>>(msg) {
                @Override
                public void success(List<PciDeviceInventory> returnValue) {
                    reply.setInventories(getAccountCanAccessPciDevices(returnValue, msg.getSession()));
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setSuccess(false);
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        }
    }

    private List<String> getIommuEnabledAndActiveHosts(List<String> hostUuids) {
        HostIommuGetter getter = new HostIommuGetter();
        return hostUuids.stream()
                .filter(hostUuid -> getter.getState(hostUuid).equals(HostIommuStateType.Enabled))
                .filter(hostUuid -> getter.getStatus(hostUuid).equals(HostIommuStatusType.Active))
                .collect(Collectors.toList());
    }

    /**
     * @param vmUuid   null for new create vm
     */
    private List<PciDeviceVO> getAttachablePciDevicesForVm(String vmUuid, List<String> hostUuids, List<PciDeviceType> types) {
        hostUuids = getIommuEnabledAndActiveHosts(hostUuids);
        if (hostUuids == null || hostUuids.isEmpty()) {
            return new ArrayList<>();
        }

        Q sql = Q.New(PciDeviceVO.class)
                .in(PciDeviceVO_.hostUuid, hostUuids)
                .in(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.attachablePciDeviceVirtStatus)
                .eq(PciDeviceVO_.passThroughState, PciDevicePassThroughState.Enabled)
                .eq(PciDeviceVO_.state, PciDeviceState.Enabled);
        if (types != null && !types.isEmpty()) {
            sql = sql.in(PciDeviceVO_.type, types);
        }
        List<PciDeviceVO> pcis = sql.list();
        return pcis.stream()
                .filter(pci -> (pci.getStatus().isAttachable()
                        || pci.getStatus().equals(PciDeviceStatus.Reserved) && pci.getVmInstanceUuid().equals(vmUuid)))
                .filter(pci -> !isAttachableEthernetPciDevice(pci))
                .collect(Collectors.toList());
    }

    private List<PciDeviceVO> getAttachablePciDevicesForVmBindPciSpec(String vmUuid, List<String> hostUuids,
                                                                      List<PciDeviceType> types, List<String> pciSpecUuids) {
        hostUuids = getIommuEnabledAndActiveHosts(hostUuids);
        if (hostUuids == null || hostUuids.isEmpty()) {
            return new ArrayList<>();
        }
        Q sql = Q.New(PciDeviceVO.class)
                .in(PciDeviceVO_.hostUuid, hostUuids)
                .in(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.attachablePciDeviceVirtStatus)
                .in(PciDeviceVO_.pciSpecUuid, pciSpecUuids)
                .eq(PciDeviceVO_.state, PciDeviceState.Enabled);
        if (types != null && !types.isEmpty()) {
            sql = sql.in(PciDeviceVO_.type, types);
        }
        List<PciDeviceVO> pcis = sql.list();
        return pcis.stream()
                .filter(pci -> (pci.getStatus().isAttachable()
                        || pci.getStatus().equals(PciDeviceStatus.Reserved) && pci.getVmInstanceUuid().equals(vmUuid)))
                .collect(Collectors.toList());
    }

    private void getPciDeviceCandidatesForStoppedVm(String vmInstanceUuid, ReturnValueCompletion<List<PciDeviceInventory>> completion) {
        getPciDeviceCandidatesForStoppedVm(vmInstanceUuid, null, completion);
    }

    private void getPciDeviceCandidatesForStoppedVm(String vmInstanceUuid, List<PciDeviceType> types, ReturnValueCompletion<List<PciDeviceInventory>> completion) {
        List<PciDeviceVO> pciDeviceVOS = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.vmInstanceUuid, vmInstanceUuid)
                .eq(PciDeviceVO_.status, PciDeviceStatus.Attached) // not include Reserved
                .list();
        if (pciDeviceVOS != null && !pciDeviceVOS.isEmpty()) {
            List<PciDeviceVO> vos = getAttachablePciDevicesForVm(vmInstanceUuid, asList(pciDeviceVOS.get(0).getHostUuid()), types);
            logger.debug(String.format("get pci device candidates[%s] for vm[uuid:%s]",
                    vos.stream().map(ResourceVO::getUuid).collect(Collectors.toList()), vmInstanceUuid));
            completion.success(PciDeviceInventory.valueOf(vos));
            return;
        }

        GetVmStartingCandidateClustersHostsMsg gmsg = new GetVmStartingCandidateClustersHostsMsg();
        gmsg.setUuid(vmInstanceUuid);
        bus.makeLocalServiceId(gmsg, VmInstanceConstant.SERVICE_ID);
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply re) {
                if (!re.isSuccess()) {
                    completion.fail(re.getError());
                } else {
                    GetVmStartingCandidateClustersHostsReply greply = (GetVmStartingCandidateClustersHostsReply) re;
                    List<PciDeviceVO> vos = getAttachablePciDevicesForVm(vmInstanceUuid, greply.getHostInventories().stream()
                            .map(HostInventory::getUuid).collect(Collectors.toList()), types);
                    logger.debug(String.format("get pci device candidates[%s] for vm[uuid:%s]",
                            vos.stream().map(ResourceVO::getUuid).collect(Collectors.toList()), vmInstanceUuid));
                    completion.success(PciDeviceInventory.valueOf(vos));
                }
            }
        });
    }

    private void handle(APIUpdateHostIommuStateMsg msg) {
        APIUpdateHostIommuStateEvent event = new APIUpdateHostIommuStateEvent(msg.getId());
        if (PciDeviceSystemTags.HOST_IOMMU_STATE.hasTag(msg.getUuid())) {
            PciDeviceSystemTags.HOST_IOMMU_STATE.delete(msg.getUuid());
        }

        String architecture = dbf.findByUuid(msg.getUuid(), HostVO.class).getArchitecture();
        SyncPciDeviceMsg smsg = new SyncPciDeviceMsg();
        smsg.setHostUuid(msg.getUuid());
        smsg.setSkipGrubConfig(CpuArchitecture.aarch64.toString().equals(architecture));

        if (msg.getState().equals(PciDeviceState.Enabled.toString())) {
            SystemTagCreator creator = PciDeviceSystemTags.HOST_IOMMU_STATE.newSystemTagCreator(msg.getUuid());
            creator.ignoreIfExisting = false;
            creator.inherent = false;
            creator.setTagByTokens(
                    map(
                            e(PciDeviceSystemTags.HOST_IOMMU_STATE_TOKEN, msg.getState())
                    )
            );
            creator.create();

            smsg.setHostIommuState(HostIommuStateType.Enabled.toString());
            bus.makeTargetServiceIdByResourceUuid(smsg, PciDeviceConstants.SERVICE_ID, msg.getUuid());
            bus.send(smsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        PciDeviceSystemTags.HOST_IOMMU_STATE.delete(msg.getUuid());
                        event.setError(reply.getError());
                    } else {
                        event.setState(new HostIommuGetter().getState(msg.getUuid()));
                    }
                    bus.publish(event);
                }
            });
        } else {
            smsg.setHostIommuState(HostIommuStateType.Disabled.toString());
            bus.makeTargetServiceIdByResourceUuid(smsg, PciDeviceConstants.SERVICE_ID, msg.getUuid());
            bus.send(smsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    event.setState(new HostIommuGetter().getState(msg.getUuid()));
                    bus.publish(event);
                }
            });
        }
    }

    private void handle(APIGetHostIommuStatusMsg msg) {
        APIGetHostIommuStatusReply reply = new APIGetHostIommuStatusReply();
        reply.setStatus(new HostIommuGetter().getStatus(msg.getUuid()));

        bus.reply(msg, reply);
    }

    private void handle(APIGetHostIommuStateMsg msg) {
        APIGetHostIommuStateReply reply = new APIGetHostIommuStateReply();
        reply.setState(new HostIommuGetter().getState(msg.getUuid()));

        bus.reply(msg, reply);
    }

    private void handle(SyncPciDeviceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return PciDeviceConstants.SYNC_PCI_DEVICE_SIGNATURE;
            }

            @Override
            public void run(SyncTaskChain chain) {
                SyncPciDeviceReply reply = new SyncPciDeviceReply();
                PciDeviceBackend bkd = getPciDeviceBackendByHostUuid(msg.getHostUuid());

                if (bkd == null) {
                    logger.debug("no pci device hypervisor backend found, no need to sync pci devices from host");
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }
                if (CpuArchitecture.aarch64.toString().equals(dbf.findByUuid(msg.getHostUuid(), HostVO.class).getArchitecture())){
                    msg.setSkipGrubConfig(true);
                }

                bkd.syncPciDeviceFromHost(msg.getHostUuid(), msg.getPciDeviceAddresses(), msg.getHostIommuState(), msg.isSkipGrubConfig(), new Completion(msg) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
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
                return getSyncSignature();
            }
        });

    }

    private void handle(DetachPciDeviceFromVmMsg msg) {
        DetachPciDeviceFromVmReply reply = new DetachPciDeviceFromVmReply();
        String vmInstanceUuid = msg.getVmInstanceUuid();
        String pciDeviceUuid = msg.getPciDeviceUuid();
        PciDeviceBackend bkd = getPciDeviceBackendByVmUuid(vmInstanceUuid);
        if (bkd == null) {
            bus.reply(msg, reply);
            return;
        }

        PciDeviceInventory inv = PciDeviceInventory.valueOf(dbf.findByUuid(pciDeviceUuid, PciDeviceVO.class));
        bkd.detachPciDeviceFromVm(inv, vmInstanceUuid, new Completion(reply) {
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

    private void handle(AttachPciDeviceToHostMsg msg) {
        AttachPciDeviceToHostReply reply = new AttachPciDeviceToHostReply();
        PciDeviceVO pci = dbf.findByUuid(msg.getPciDeviceUuid(), PciDeviceVO.class);
        PciDeviceInventory inv = PciDeviceInventory.valueOf(pci);
        PciDeviceBackend bkd = getPciDeviceBackendByHostUuid(pci.getHostUuid());
        bkd.attachPciDeviceToHost(inv, new Completion(reply) {
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

    private void handle(DetachPciDeviceFromHostMsg msg) {
        DetachPciDeviceFromHostReply reply = new DetachPciDeviceFromHostReply();
        PciDeviceVO pci = dbf.findByUuid(msg.getPciDeviceUuid(), PciDeviceVO.class);
        PciDeviceInventory inv = PciDeviceInventory.valueOf(pci);
        PciDeviceBackend bkd = getPciDeviceBackendByHostUuid(pci.getHostUuid());
        bkd.detachPciDeviceFromHost(inv, new Completion(reply) {
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

    private void handle(CheckAndReservePciDeviceBySpecMsg msg) {
        CheckAndReservePciDeviceBySpecReply reply = new CheckAndReservePciDeviceBySpecReply();
        thdf.chainSubmit(new ChainTask(reply) {
            @Override
            public String getSyncSignature() {
                return PciDeviceConstants.CHECK_AND_RESERVE_PCI_DEVICE_FOR_VM;
            }

            @Override
            public void run(SyncTaskChain chain) {
                String hostUuid = msg.getHostUuid();
                String vmUuid = msg.getVmUuid();

                List<PciDeviceVO> candidates = new ArrayList<>();
                Map<String, Integer> specMap = PciDeviceUtils.getVmPciSpecUuids(vmUuid);

                List<String> accessiblePciUuids = new ArrayList<>();
                String accountUuid = acntMgr.getOwnerAccountUuidOfResource(vmUuid);
                if (!accountUuid.equals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)) {
                    // QuotaChecker cannot handle the scenario of normal account batch creating vms with pci/mdev specs.
                    // New created vms allocate host one by one, so we can check quota again here.
                    // The request info is transform from system tags into database, just `getUsedPci` from db and check.
                    Map<String, Quota.QuotaPair> pairs = new QuotaUtil().makeQuotaPairs(accountUuid);
                    for (PciDeviceType deviceType : PciDeviceType.leagalPciDeviceCandidateTypes) {
                        try {
                            PciDeviceQuotaUtils.check(accountUuid, deviceType.toString(), 0L, pairs);
                        } catch (RuntimeException e) {
                            reply.setError(operr(e.getMessage()));
                            bus.reply(msg, reply);
                            chain.next();
                            return;
                        }
                    }

                    accessiblePciUuids = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, PciDeviceVO.class);
                    if (CollectionUtils.isEmpty(accessiblePciUuids)) {
                        accessiblePciUuids = Collections.singletonList(Platform.FAKE_UUID);
                    }
                }

                for (Map.Entry<String, Integer> specMapEntry : specMap.entrySet()) {
                    String specUuid = specMapEntry.getKey();
                    int deviceNum = specMapEntry.getValue();

                    List<PciDeviceVO> pcis = Q.New(PciDeviceVO.class)
                            .eq(PciDeviceVO_.hostUuid, hostUuid)
                            .eq(PciDeviceVO_.pciSpecUuid, specUuid)
                            .eq(PciDeviceVO_.state, PciDeviceState.Enabled)
                            .in(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.attachablePciDeviceVirtStatus)
                            .list();

                    int attachedNum = (int) pcis.stream().filter(pci ->
                            PciDeviceStatus.Attached.equals(pci.getStatus())
                                && vmUuid.equals(pci.getVmInstanceUuid())
                                && PciDeviceChooser.Spec.equals(pci.getChooser())
                    ).count();

                    if (deviceNum < attachedNum) {
                        logger.error(String.format("more than %d pci devices related to spec[uuid:%s] are attached to vm[uuid:%s]",
                                deviceNum, specUuid, vmUuid));
                    }

                    // still need `deviceNum` pcis of `specUuid` for vm
                    deviceNum -= attachedNum;
                    if (deviceNum == 0) {
                        continue;
                    }

                    // find all available pcis in dest host, filter out pcis that are reserved by other vm
                    final List<String> finalAccessiblePciUuids = accessiblePciUuids;
                    List<PciDeviceVO> avaliablePcis = pcis.stream().filter(
                        pci -> pci.getStatus().isAttachable() &&
                                (accountUuid.equals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID) || finalAccessiblePciUuids.contains(pci.getUuid()))
                    ).collect(Collectors.toList());
                    Map<String, List<String>> iommuGroupMap = avaliablePcis.stream().collect(Collectors.toMap(
                            PciDeviceVO::getUuid,
                            pci -> PciDeviceUtils.getPciDeviceUuidsFromSameIommuGroup(pci.getUuid())));
                    iommuGroupMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());

                    if (iommuGroupMap.size() < deviceNum) {
                        reply.setError(Platform.operr("failed to find enough pci device of spec[uuid:%s] in dest host[uuid:%s] for vm[uuid:%s]",
                                specUuid, hostUuid, vmUuid));
                        bus.reply(msg, reply);
                        chain.next();
                        return;
                    }

                    // add to candidates
                    List<String> pciUuids = iommuGroupMap.entrySet().stream()
                            .limit(deviceNum)
                            .map(Map.Entry::getValue)
                            .flatMap(List::stream)
                            .collect(Collectors.toList());
                    candidates.addAll(Q.New(PciDeviceVO.class).in(PciDeviceVO_.uuid, pciUuids).list());
                }

                // destHost has enough spec related pci devices for vm
                List<String> reservedPciDevices = new ArrayList<>();
                if (msg.isDryRun()) {
                    logger.debug(String.format("host[uuid:%s] has enough spec related pci devices for vm[uuid:%s], " +
                            "they are not reserved because dry run", hostUuid, vmUuid));
                    reply.setReservedPciDevices(reservedPciDevices);
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                for (PciDeviceVO candidate : candidates) {
                    candidate.setVmInstanceUuid(vmUuid);
                    candidate.setStatus(PciDeviceStatus.Reserved);
                    candidate.setChooser(PciDeviceChooser.Spec);
                }
                dbf.updateCollection(candidates);

                downloadPciDeviceRomToHostForVm(hostUuid, vmUuid);
                logger.debug(String.format("reserved pci devices[uuid:%s] on host[uuid:%s] for vm[uuid:%s]", reservedPciDevices, hostUuid, vmUuid));
                reply.setReservedPciDevices(reservedPciDevices);
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(CheckAndReservePciDeviceMsg msg) {
        CheckAndReservePciDeviceReply reply = new CheckAndReservePciDeviceReply();
        thdf.chainSubmit(new ChainTask(reply) {
            @Override
            public String getSyncSignature() {
                return PciDeviceConstants.CHECK_AND_RESERVE_PCI_DEVICE_FOR_VM;
            }

            @Override
            public void run(SyncTaskChain chain) {
                // check
                String vmUuid = msg.getVmUuid();
                String pciUuid = msg.getPciUuid();
                List<String> pciUuids = PciDeviceUtils.getPciDeviceUuidsFromSameIommuGroup(pciUuid);
                if (pciUuids.isEmpty()) {
                    reply.setError(operr("something wrong with iommu group of pci device[uuid:%s]", pciUuid));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                List<PciDeviceVO> pcis = Q.New(PciDeviceVO.class)
                        .in(PciDeviceVO_.uuid, pciUuids)
                        .eq(PciDeviceVO_.state, PciDeviceState.Enabled)
                        .in(PciDeviceVO_.status, PciDeviceStatus.attachablePciDeviceStatus)
                        .in(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.attachablePciDeviceVirtStatus)
                        .list();
                if (pcis.size() != pciUuids.size()) {
                    reply.setError(operr("pci devices [%s] are not all available", pciUuids));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                // reserve
                List<String> reservedPciDevices = new ArrayList<>();
                for (PciDeviceVO pci : pcis) {
                    pci.setVmInstanceUuid(vmUuid);
                    pci.setStatus(PciDeviceStatus.Reserved);
                    pci.setChooser(PciDeviceChooser.Device);
                    reservedPciDevices.add(pci.getUuid());
                }
                dbf.updateCollection(pcis);

                logger.debug(String.format("checked and reserved pci device[uuid:%s] for vm[uuid:%s]", pciUuid, vmUuid));
                reply.setReservedPciDevices(reservedPciDevices);
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(APIAttachPciDeviceToVmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {

            @Override
            public void run(SyncTaskChain syncTaskChain) {
                apiAttachPciDeviceToVm(msg, new Completion(syncTaskChain) {
                    @Override
                    public void success() {
                        syncTaskChain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        syncTaskChain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public String getName() {
                return String.format("update-pci-device-to-vm-%s",msg.getVmInstanceUuid());
            }
        });
    }

    private void apiAttachPciDeviceToVm(APIAttachPciDeviceToVmMsg msg, Completion completion) {
        APIAttachPciDeviceToVmEvent event = new APIAttachPciDeviceToVmEvent(msg.getId());
        String vmUuid = msg.getVmInstanceUuid();
        String pciUuid = msg.getPciDeviceUuid();
        List<String> pciUuids = PciDeviceUtils.getPciDeviceUuidsFromSameIommuGroup(pciUuid);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("attach-pci-devices-of-same-group-as-%s-to-vm-%s", pciUuid, vmUuid));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(checkAndReservePciDeviceFlow(vmUuid, pciUuid));
                flow(createDetachPciDeviceFromHostFlow(pciUuids));
                flow(createAttachPciDeviceToVmFlow(pciUuids, vmUuid));
                //flow(createSyncPciDeviceInfoFlow(pciUuids));

                done(new FlowDoneHandler(event) {
                    @Override
                    public void handle(Map data) {
                        VmInstancePciDeviceSpecRefVO ref = SQL.New("select ref from VmInstancePciDeviceSpecRefVO ref, PciDeviceVO pci " +
                                        "where ref.vmInstanceUuid = :vmUuid and pci.pciSpecUuid = ref.pciSpecUuid " +
                                        "and pci.uuid = :pciUuid", VmInstancePciDeviceSpecRefVO.class)
                                .param("vmUuid", vmUuid)
                                .param("pciUuid", pciUuid)
                                .find();
                        if (ref != null) {
                            updatePcideviceNumber(ref, PciDeviceConstants.OPERATION_ADD);
                        }

                        event.setInventory(PciDeviceInventory.valueOf(dbf.findByUuid(pciUuid, PciDeviceVO.class)));
                        bus.publish(event);
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(event) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        event.setError(errCode);
                        bus.publish(event);
                        completion.success();
                    }
                });
            }
        }).start();
    }

    private void updatePcideviceNumber(VmInstancePciDeviceSpecRefVO ref, String operation) {
        GLock lock = new GLock(String.format("update-pci-device-number-on-ref-%s", ref.getId()),
                TimeUnit.MINUTES.toSeconds(30));
        lock.lock();
        try {
            if (PciDeviceConstants.OPERATION_ADD.equals(operation)) {
                ref.setPciDeviceNumber(ref.getPciDeviceNumber() + 1);
                dbf.update(ref);

            } else if (PciDeviceConstants.OPERATION_SUB.equals(operation)) {
                ref.setPciDeviceNumber(ref.getPciDeviceNumber() - 1);
                dbf.update(ref);
            }
        } finally {
            lock.unlock();
        }
    }

    private void handle(AttachPciDeviceToVmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                AttachPciDeviceToVmReply reply = new AttachPciDeviceToVmReply();
                attachPciDeviceToVm(msg, new Completion(msg) {
                    @Override
                    public void success() {
                        // update PciDeviceVO right after `virsh attach-device` succeeded
                        PciDeviceVO pci = dbf.findByUuid(msg.getPciDeviceUuid(), PciDeviceVO.class);
                        pci.setStatus(PciDeviceStatus.Attached);
                        pci.setVmInstanceUuid(msg.getVmInstanceUuid());
                        dbf.updateAndRefresh(pci);

                        PciDeviceCanonicalEvents.PciDeviceStateChangedData cdata = new PciDeviceCanonicalEvents.PciDeviceStateChangedData();
                        cdata.setVmUuid(msg.getVmInstanceUuid());
                        cdata.setPciDeviceUuid(msg.getPciDeviceUuid());
                        cdata.setHostUuid(pci.getHostUuid());
                        cdata.setStatus(pci.getStatus().toString());
                        cdata.setInventory(PciDeviceInventory.valueOf(pci));
                        cdata.setDescription(pci.getDescription());
                        evtf.fire(PciDeviceCanonicalEvents.PCIDEVICE_FULL_STATE_CHANGED_PATH, cdata);

                        PciDeviceUtils.cleanUpReserveTags(msg.getVmInstanceUuid(), Collections.singletonList(msg.getPciDeviceUuid()));
                        bus.reply(msg, reply);
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
                return String.format("attach-pci-device-to-vm-%s",msg.getVmInstanceUuid());
            }
        });
    }

    private void attachPciDeviceToVm(AttachPciDeviceToVmMsg msg, Completion completion) {
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        PciDeviceVO vo = Q.New(PciDeviceVO.class).eq(PciDeviceVO_.uuid, msg.getPciDeviceUuid()).find();
        PciDeviceInventory inv = PciDeviceInventory.valueOf(vo);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("attach-pci-device-%s-to-vm-%s", msg.getPciDeviceUuid(), msg.getVmInstanceUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = String.format("check-pci-device-%s-attachable-if-vm-%s-stopped", msg.getPciDeviceUuid(), msg.getVmInstanceUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (!vmInstanceVO.getState().equals(VmInstanceState.Stopped)) {
                    trigger.next();
                    return;
                }
                getPciDeviceCandidatesForStoppedVm(msg.getVmInstanceUuid(), new ReturnValueCompletion<List<PciDeviceInventory>>(msg) {
                    @Override
                    public void success(List<PciDeviceInventory> returnValue) {
                        if (returnValue.stream().noneMatch(i -> i.getUuid().equals(msg.getPciDeviceUuid()))) {
                            trigger.fail(operr("can not attach this pci device[uuid:%s] to vm[uuid:%s] due to host allocation",
                                    msg.getPciDeviceUuid(), msg.getVmInstanceUuid()));
                            return;
                        }
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("attach-pci-device-%s-to-vm-%s-in-backend", msg.getPciDeviceUuid(), msg.getVmInstanceUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                PciDeviceBackend bkd = getPciDeviceBackendByVmUuid(msg.getVmInstanceUuid());
                if (bkd == null) {
                    trigger.fail(operr("vm[%s] does not exist, cannot bind to pci[%s]", msg.getVmInstanceUuid(), inv.getUuid()));
                    return;
                }

                bkd.attachPciDeviceToVm(inv, msg.getVmInstanceUuid(), new Completion(msg) {
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
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).start();
    }

    private void handle(APIDetachPciDeviceFromVmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                apiDetachPciDeviceFromVm(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public String getName() {
                return String.format("update-pci-device-to-vm-%s",msg.getVmInstanceUuid());
            }
        });
    }

    private void apiDetachPciDeviceFromVm(APIDetachPciDeviceFromVmMsg msg, Completion completion) {
        APIDetachPciDeviceFromVmEvent event = new APIDetachPciDeviceFromVmEvent(msg.getId());
        List<String> pciUuids = PciDeviceUtils.getPciDeviceUuidsFromSameIommuGroup(msg.getPciDeviceUuid(), true);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("detach-pci-devices-of-same-group-as-" + msg.getPciDeviceUuid());
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(createDetachPciDeviceFromVmFlow(pciUuids));
                flow(createAttachPciDeviceToHostFlow(pciUuids));
                //flow(createSyncPciDeviceInfoFlow(pciUuids));

                done(new FlowDoneHandler(event) {
                    @Override
                    public void handle(Map data) {
                        detachPciDeviceFromVmInDb(pciUuids);
                        VmInstancePciDeviceSpecRefVO ref = SQL.New("select ref from VmInstancePciDeviceSpecRefVO ref, PciDeviceVO pci " +
                                        "where ref.vmInstanceUuid = :vmUuid and pci.pciSpecUuid = ref.pciSpecUuid " +
                                        "and pci.uuid = :pciUuid", VmInstancePciDeviceSpecRefVO.class)
                                .param("vmUuid", msg.getVmInstanceUuid())
                                .param("pciUuid", msg.getPciDeviceUuid())
                                .find();
                        if (ref != null && ref.getPciDeviceNumber() > 0) {
                            updatePcideviceNumber(ref, PciDeviceConstants.OPERATION_SUB);
                        }

                        event.setInventory(PciDeviceInventory.valueOf(dbf.findByUuid(msg.getPciDeviceUuid(), PciDeviceVO.class)));
                        bus.publish(event);
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(event) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        event.setError(errCode);
                        bus.publish(event);
                        completion.success();
                    }
                });
            }
        }).start();
    }

    private void handle(APICreatePciDeviceOfferingMsg msg) {
        APICreatePciDeviceOfferingEvent event = new APICreatePciDeviceOfferingEvent(msg.getId());
        PciDeviceOfferingVO vo = new PciDeviceOfferingVO();
        vo.setUuid(msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setVendorId(msg.getVendorId());
        vo.setDeviceId(msg.getDeviceId());
        vo.setSubvendorId(msg.getSubvendorId());
        vo.setSubdeviceId(msg.getSubdeviceId());
        vo.setType(PciDeviceOfferingType.Generic);
        dbf.persist(vo);
        syncPciDevicePciDeviceOfferingRef(vo);
        event.setInventory(PciDeviceOfferingInventory.valueOf(vo));
        bus.publish(event);
    }

    private void syncPciDevicePciDeviceOfferingRef(PciDeviceOfferingVO vo) {
        List<PciDeviceVO> deviceVOS = Q.New(PciDeviceVO.class).list();

        deviceVOS.forEach(pci -> {
            if (vo.matchPciDevice(pci)) {
                if (!Q.New(PciDevicePciDeviceOfferingRefVO.class)
                        .eq(PciDevicePciDeviceOfferingRefVO_.pciDeviceOfferingUuid, vo.getUuid())
                        .eq(PciDevicePciDeviceOfferingRefVO_.pciDeviceUuid, pci.getUuid()).isExists()) {
                    PciDevicePciDeviceOfferingRefVO refVO = new PciDevicePciDeviceOfferingRefVO();
                    refVO.setPciDeviceOfferingUuid(vo.getUuid());
                    refVO.setPciDeviceUuid(pci.getUuid());
                    dbf.persist(refVO);
                }
            }
        });
    }

    private void handle(APIDeletePciDeviceOfferingMsg msg) {
        APIDeletePciDeviceOfferingEvent event = new APIDeletePciDeviceOfferingEvent(msg.getId());
        PciDeviceOfferingVO vo = Q.New(PciDeviceOfferingVO.class).eq(PciDeviceOfferingVO_.uuid, msg.getUuid()).find();
        dbf.remove(vo);
        bus.publish(event);
    }

    private void handle(APIDeletePciDeviceMsg msg) {
        APIDeletePciDeviceEvent event = new APIDeletePciDeviceEvent(msg.getId());
        PciDeviceVO vo = Q.New(PciDeviceVO.class).eq(PciDeviceVO_.uuid, msg.getUuid()).find();
        dbf.remove(vo);
        bus.publish(event);
    }

    private void handle(APIUpdatePciDeviceMsg msg) {
        APIUpdatePciDeviceEvent event = new APIUpdatePciDeviceEvent(msg.getId());
        PciDeviceVO vo = Q.New(PciDeviceVO.class).eq(PciDeviceVO_.uuid, msg.getUuid()).find();

        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
        }

        if (msg.getName() != null) {
            vo.setName(msg.getName());
        }

        if (msg.getMetaData() != null) {
            vo.setMetaData(msg.getMetaData());
        }

        if (msg.getState() != null) {
            vo.setState(PciDeviceState.valueOf(msg.getState()));
        }

        if (msg.getPassThroughState() != null) {
            vo.setPassThroughState(PciDevicePassThroughState.valueOf(msg.getPassThroughState()));
        }

        // update state, passThroughState of all the other pci devices in group
        List<String> uuids = PciDeviceUtils.getPciDeviceUuidsFromSameIommuGroup(vo.getUuid());
        if (!uuids.isEmpty()) {
            SQL.New(PciDeviceVO.class)
                    .in(PciDeviceVO_.uuid, uuids)
                    .notEq(PciDeviceVO_.uuid, vo.getUuid())
                    .set(PciDeviceVO_.state, vo.getState())
                    .set(PciDeviceVO_.passThroughState, vo.getPassThroughState())
                    .update();
        }

        dbf.updateAndRefresh(vo);
        event.setInventory(PciDeviceInventory.valueOf(vo));
        bus.publish(event);
    }

    @Override
    public VmInstanceType getVmTypeForAddonExtension() {
        return VmInstanceType.valueOf(VmInstanceConstant.USER_VM_TYPE);
    }

    @Override
    public void addAddon(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        String vmUuid = spec.getVmInventory().getUuid();

        if (spec.getVmInventory().getType().equals(ApplianceVmConstant.APPLIANCE_VM_TYPE)) {
            return;
        }

        boolean gpuExist = Q.New(GpuDeviceVO.class).eq(GpuDeviceVO_.hostUuid, host.getUuid()).isExists();
        if (gpuExist && !cmd.getBootMode().equals(ImageBootMode.Legacy.toString())) {
            cmd.setQemu64BitPciMmioSetup(true);
        }

        List<PciDeviceVO> vos = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.vmInstanceUuid, vmUuid)
                .eq(PciDeviceVO_.status, PciDeviceStatus.Attached)
                .list();

        // pci devices associated to vf nics are not handled here
        vos = vos.stream().filter(vo -> vo.getVirtStatus() != PciDeviceVirtStatus.SRIOV_VIRTUAL ||
                vo.getType() != PciDeviceType.Ethernet_Controller).collect(Collectors.toList());

        if (vos.isEmpty()) {
            return;
        }

        List<String> pciAddons = vos.stream()
                .sorted(Comparator.comparing(PciDeviceVO::getIommuGroup).thenComparing(PciDeviceVO::getPciDeviceAddress))
                .map(vo -> String.format("%s,%s", vo.getPciDeviceAddress(), vo.getPciSpecUuid()))
                .collect(Collectors.toList());

        cmd.getAddons().put(PciDeviceConstants.SERVICE_ID, pciAddons);
        logger.debug(String.format("add pci devices %s to xml of vm instance[uuid:%s] in org.zstack.pciDevice.PciDeviceManager.addAddon",
                pciAddons, vmUuid));
    }

    @Override
    public void kvmReportVmShutdownEvent(ShutdownDetail detail) {
        final String vmUuid = detail.vmUuid;
        // make sure pci devices are automatically detached (if need to) when vm is stopped bypass zstack
        VmInstanceState state = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.state)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .findValue();
        if (state == VmInstanceState.Rebooting) {
            return;
        }

        detachPciDevicesFromVm(vmUuid, false,
                PciDeviceUtils.releaseSpecReleatedPhysicalPciDevicesWhenStop(vmUuid),
                PciDeviceUtils.releaseSpecReleatedVirtualPciDevicesWhenStop(vmUuid),
                new Completion(null) {
                    @Override
                    public void success() {
                        logger.debug(String.format("auto detached pci devices from vm[uuid:%s] because it's been stopped bypass zstack", vmUuid));
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.error(String.format("failed to auto detach pci devices from vm[uuid:%s] because it's been stopped bypass zstack", vmUuid));
                    }
                });

        // tell mdev device manager that libvirt report vm shutdown
        evf.fire(VmCanonicalEvents.VM_LIBVIRT_REPORT_SHUTDOWN, vmUuid);
    }


    @Override
    public boolean start() {
        populateExtensions();
        syncPciDeviceOfferingRef();

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(PciDeviceConstants.SERVICE_ID);
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(APIMigrateVmMsg.class,
                APIPrimaryStorageMigrateVmMsg.class,
                APIPrimaryStorageMigrateVolumeMsg.class,
                APILocalStorageMigrateVolumeMsg.class,
                APICreateVmInstanceMsg.class,
                APIShareResourceMsg.class,
                APIRevokeResourceSharingMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIMigrateVmMsg) {
            validate((APIMigrateVmMsg) msg);
        } else if (msg instanceof APILocalStorageMigrateVolumeMsg) {
            validate((APILocalStorageMigrateVolumeMsg) msg);
        } else if (msg instanceof APIPrimaryStorageMigrateVolumeMsg) {
            validate((APIPrimaryStorageMigrateVolumeMsg) msg);
        } else if (msg instanceof APIPrimaryStorageMigrateVmMsg) {
            validate((APIPrimaryStorageMigrateVmMsg) msg);
        } else if (msg instanceof APICreateVmInstanceMsg) {
            validate((APICreateVmInstanceMsg) msg);
        } else if (msg instanceof APIShareResourceMsg) {
            validate((APIShareResourceMsg) msg);
        } else if (msg instanceof APIRevokeResourceSharingMsg) {
            validate((APIRevokeResourceSharingMsg) msg);
        }

        return msg;
    }

    @Transactional(readOnly = true)
    private void validate(APIMigrateVmMsg msg) {
        List<String> pciUuids = Q.New(VmVdpaNicVO.class)
                .eq(VmVdpaNicVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .select(VmVdpaNicVO_.pciDeviceUuid)
                .listValues();
        List<String> vfPciUuids = Q.New(VmVfNicVO.class)
                .eq(VmVfNicVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .select(VmVfNicVO_.pciDeviceUuid)
                .listValues();
        pciUuids.addAll(vfPciUuids);

        Q query = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.vmInstanceUuid, msg.getVmInstanceUuid());

        if (!pciUuids.isEmpty()) {
            query = query.notIn(PciDeviceVO_.uuid, pciUuids);
        }
        List<PciDeviceVO> pciDeviceVOS = query.list();
        if (pciDeviceVOS != null && !pciDeviceVOS.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("can not migrate vm[uuid:%s] since pci device attached",
                    msg.getVmInstanceUuid()));
        }
    }

    @Transactional(readOnly = true)
    private void validate(APILocalStorageMigrateVolumeMsg msg) {
        VolumeVO volume = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        if (volume != null && volume.getType() == VolumeType.Root) {
            List<String> pciUuids = Q.New(VmVdpaNicVO.class)
                    .eq(VmVdpaNicVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                    .select(VmVdpaNicVO_.pciDeviceUuid)
                    .listValues();

            Q query = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.vmInstanceUuid, msg.getVmInstanceUuid());

            if (!pciUuids.isEmpty()) {
                query = query.notIn(PciDeviceVO_.uuid, pciUuids);
            }
            boolean pciAttached = query.isExists();

            if (pciAttached) {
                throw new ApiMessageInterceptionException(Platform.operr(
                        "cannot migrate root volume[uuid:%s] because there are pci devices attached",
                        msg.getVolumeUuid()
                ));
            }
        }
    }

    @Transactional(readOnly = true)
    private void validate(APIPrimaryStorageMigrateVolumeMsg msg) {
        VolumeVO volume = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        if (volume != null && volume.getType() == VolumeType.Root) {
            List<String> pciUuids = Q.New(VmVdpaNicVO.class)
                    .eq(VmVdpaNicVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                    .select(VmVdpaNicVO_.pciDeviceUuid)
                    .listValues();
            List<String> vfPciUuids = Q.New(VmVfNicVO.class)
                    .eq(VmVfNicVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                    .select(VmVfNicVO_.pciDeviceUuid)
                    .listValues();
            pciUuids.addAll(vfPciUuids);

            Q query = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.vmInstanceUuid, msg.getVmInstanceUuid());

            if (!pciUuids.isEmpty()) {
                query = query.notIn(PciDeviceVO_.uuid, pciUuids);
            }
            boolean pciAttached = query.isExists();

            if (pciAttached) {
                throw new ApiMessageInterceptionException(Platform.operr(
                        "cannot migrate root volume[uuid:%s] because there are pci devices attached",
                        msg.getVolumeUuid()
                ));
            }
        }
    }

    @Transactional(readOnly = true)
    private void validate(APIPrimaryStorageMigrateVmMsg msg) {
        if (msg.getVmInstanceUuid() != null) {
            List<String> pciUuids = Q.New(VmVdpaNicVO.class)
                    .eq(VmVdpaNicVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                    .select(VmVdpaNicVO_.pciDeviceUuid)
                    .listValues();
            List<String> vfPciUuids = Q.New(VmVfNicVO.class)
                    .eq(VmVfNicVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                    .select(VmVfNicVO_.pciDeviceUuid)
                    .listValues();
            pciUuids.addAll(vfPciUuids);

            Q query = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.vmInstanceUuid, msg.getVmInstanceUuid());

            if (!pciUuids.isEmpty()) {
                query = query.notIn(PciDeviceVO_.uuid, pciUuids);
            }
            boolean pciAttached = query.isExists();

            if (pciAttached) {
                throw new ApiMessageInterceptionException(Platform.operr(
                        "cannot migrate vm[uuid:%s] because there are pci devices attached",
                        msg.getVmInstanceUuid()
                ));
            }
        }
    }

    @Transactional(readOnly = true)
    private void validate(APICreateVmInstanceMsg msg) {
        List<String> systemTags = msg.getSystemTags();
        if (systemTags == null || systemTags.isEmpty()) {
            return;
        }

        List<String> pciDeviceUuids = new ArrayList<>();
        for (String tag : systemTags) {
            if (PciDeviceSystemTags.PCI_DEVICE.isMatch(tag)) {
                String pciDeviceUuid = PciDeviceSystemTags.PCI_DEVICE.getTokenByTag(tag, PciDeviceSystemTags.PCI_DEVICE_TOKEN);
                PciDeviceVO vo = dbf.findByUuid(pciDeviceUuid, PciDeviceVO.class);
                if (vo == null) {
                    throw new ApiMessageInterceptionException(Platform.argerr("pci device[uuid:%s] doesn't exists", pciDeviceUuid));
                }
                if (!PciDeviceType.leagalPciDeviceCandidateTypes.contains(vo.getType())) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "illegal type[%s] for pci device, only %s are legal",
                            vo.getType(), PciDeviceType.leagalPciDeviceCandidateTypes));
                }
                pciDeviceUuids.add(pciDeviceUuid);
            }

            if (PciDeviceSystemTags.PCI_DEVICE_SPEC.isMatch(tag)) {
                String pciSpecUuid = PciDeviceSystemTags.PCI_DEVICE_SPEC.getTokenByTag(tag, PciDeviceSystemTags.PCI_DEVICE_SPEC_UUID_TOKEN);
                PciDeviceSpecVO spec = dbf.findByUuid(pciSpecUuid, PciDeviceSpecVO.class);
                if (spec == null) {
                    throw new ApiMessageInterceptionException(Platform.argerr("pci device spec[uuid:%s] doesn't exists", pciSpecUuid));
                }
                if (!PciDeviceType.leagalPciDeviceCandidateTypes.contains(spec.getType())) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "illegal type[%s] for pci device spec, only %s are legal",
                            spec.getType(), PciDeviceType.leagalPciDeviceCandidateTypes));
                }
            }
        }

        for (String pciDeviceUuid : pciDeviceUuids) {
            List<String> otherUuids = PciDeviceUtils.getPciDeviceUuidsFromSameIommuGroup(pciDeviceUuid);
            if (otherUuids.isEmpty()) {
                throw new ApiMessageInterceptionException(Platform.argerr(PciDeviceUtils.iommuGroupError(
                                PciDeviceInventory.valueOf(dbf.findByUuid(pciDeviceUuid, PciDeviceVO.class)))));
            }

            otherUuids.remove(pciDeviceUuid);
            for (String otherUuid : otherUuids) {
                msg.getSystemTags().add(PciDeviceSystemTags.PCI_DEVICE.instantiateTag(map(
                    e(PciDeviceSystemTags.PCI_DEVICE_TOKEN, otherUuid)
                )));
            }
        }
    }

    private void validate(APIShareResourceMsg msg) {
        // all pci devices in same group should be shared
        Set<String> uuids = new HashSet<>(msg.getResourceUuids());
        for (String uuid : msg.getResourceUuids()) {
            uuids.addAll(PciDeviceUtils.getPciDeviceUuidsFromSameIommuGroup(uuid));
        }
        msg.setResourceUuids(new ArrayList<>(uuids));
    }

    private void validate(APIRevokeResourceSharingMsg msg) {
        // all pci devices in same group should be revoked
        Set<String> uuids = new HashSet<>(msg.getResourceUuids());
        for (String uuid : msg.getResourceUuids()) {
            uuids.addAll(PciDeviceUtils.getPciDeviceUuidsFromSameIommuGroup(uuid));
        }
        msg.setResourceUuids(new ArrayList<>(uuids));
    }

    @Override
    public List<String> getAddtionalResourceType() {
        return asList(PciDeviceVO.class.getName());
    }

    @Override
    public List<Quota> reportQuota() {
        Quota quota = new Quota();
        quota.defineQuota(new GenericPCIDeviceNumQuotaDefinition());
        quota.defineQuota(new GPUNumQuotaDefinition());
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateVmInstanceMsg.class)
                .addMessageRequiredQuotaHandler(PciDeviceConstants.GENERIC_PCI_DEVICE_NUMBER, (msg) -> PciDeviceQuotaUtils
                        .getRequestPciDeviceNumber(msg.getSystemTags()).get(PciDeviceConstants.Generic))
                .addMessageRequiredQuotaHandler(PciDeviceConstants.GPU_NUMBER, (msg) -> PciDeviceQuotaUtils
                        .getRequestPciDeviceNumber(msg.getSystemTags()).get(PciDeviceConstants.GPU_3D_Controller)));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIAttachPciDeviceToVmMsg.class)
                .addMessageRequiredQuotaHandler(PciDeviceConstants.GENERIC_PCI_DEVICE_NUMBER, (msg) -> getRequiredQuotaByMessage(PciDeviceConstants.GENERIC_PCI_DEVICE_NUMBER, msg))
                .addMessageRequiredQuotaHandler(PciDeviceConstants.GPU_NUMBER, (msg) -> getRequiredQuotaByMessage(PciDeviceConstants.GPU_NUMBER, msg)));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIAttachMdevDeviceToVmMsg.class)
                .addMessageRequiredQuotaHandler(PciDeviceConstants.GENERIC_PCI_DEVICE_NUMBER, (msg) -> getRequiredQuotaByMessage(PciDeviceConstants.GENERIC_PCI_DEVICE_NUMBER, msg))
                .addMessageRequiredQuotaHandler(PciDeviceConstants.GPU_NUMBER, (msg) -> getRequiredQuotaByMessage(PciDeviceConstants.GPU_NUMBER, msg)));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIAddPciDeviceSpecToVmInstanceMsg.class)
                .addMessageRequiredQuotaHandler(PciDeviceConstants.GENERIC_PCI_DEVICE_NUMBER, (msg) -> getRequiredQuotaByMessage(PciDeviceConstants.GENERIC_PCI_DEVICE_NUMBER, msg))
                .addMessageRequiredQuotaHandler(PciDeviceConstants.GPU_NUMBER, (msg) -> getRequiredQuotaByMessage(PciDeviceConstants.GPU_NUMBER, msg)));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIAddMdevDeviceSpecToVmInstanceMsg.class)
                .addMessageRequiredQuotaHandler(PciDeviceConstants.GENERIC_PCI_DEVICE_NUMBER, (msg) -> getRequiredQuotaByMessage(PciDeviceConstants.GENERIC_PCI_DEVICE_NUMBER, msg))
                .addMessageRequiredQuotaHandler(PciDeviceConstants.GPU_NUMBER, (msg) -> getRequiredQuotaByMessage(PciDeviceConstants.GPU_NUMBER, msg)));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIChangeResourceOwnerMsg.class)
                .addMessageRequiredQuotaHandler(PciDeviceConstants.GENERIC_PCI_DEVICE_NUMBER, (msg) -> getRequiredQuotaByMessage(PciDeviceConstants.GENERIC_PCI_DEVICE_NUMBER, msg))
                .addMessageRequiredQuotaHandler(PciDeviceConstants.GPU_NUMBER, (msg) -> getRequiredQuotaByMessage(PciDeviceConstants.GPU_NUMBER, msg)));

        return Collections.singletonList(quota);
    }

    private long getRequiredQuotaByMessage(String quotaName, APIMessage msg) {
        long requiredSize = 0;
        String deviceTypeStr = null;
        if (msg instanceof APIAttachPciDeviceToVmMsg) {
            PciDeviceType deviceType = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.uuid, ((APIAttachPciDeviceToVmMsg) msg).getPciDeviceUuid())
                    .select(PciDeviceVO_.type)
                    .findValue();
            deviceTypeStr = deviceType == null ? null : deviceType.toString();
            requiredSize = 1;
        } else if (msg instanceof APIAddPciDeviceSpecToVmInstanceMsg) {
            PciDeviceType deviceType = Q.New(PciDeviceSpecVO.class)
                    .eq(PciDeviceSpecVO_.uuid, ((APIAddPciDeviceSpecToVmInstanceMsg) msg).getPciSpecUuid())
                    .select(PciDeviceSpecVO_.type)
                    .findValue();
            deviceTypeStr = deviceType == null ? null : deviceType.toString();
            requiredSize = ((APIAddPciDeviceSpecToVmInstanceMsg) msg).getPciDeviceNumber();
        } else if (msg instanceof APIAttachMdevDeviceToVmMsg) {
            MdevDeviceType deviceType = Q.New(MdevDeviceVO.class)
                    .eq(MdevDeviceVO_.uuid, ((APIAttachMdevDeviceToVmMsg) msg).getMdevDeviceUuid())
                    .select(MdevDeviceVO_.type)
                    .findValue();
            deviceTypeStr = deviceType == null ? null : deviceType.toString();
            requiredSize = 1;
        } else if (msg instanceof APIAddMdevDeviceSpecToVmInstanceMsg) {
            MdevDeviceType deviceType = Q.New(MdevDeviceSpecVO.class)
                    .eq(MdevDeviceSpecVO_.uuid, ((APIAddMdevDeviceSpecToVmInstanceMsg) msg).getMdevSpecUuid())
                    .select(MdevDeviceSpecVO_.type)
                    .findValue();
            deviceTypeStr = deviceType == null ? null : deviceType.toString();
            requiredSize = ((APIAddMdevDeviceSpecToVmInstanceMsg) msg).getMdevDeviceNumber();
        } else if (msg instanceof APIChangeResourceOwnerMsg) {
            String resourceUuid = ((APIChangeResourceOwnerMsg) msg).getResourceUuid();
            PciDeviceType pciDeviceType = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.uuid, resourceUuid)
                    .select(PciDeviceVO_.type)
                    .findValue();
            if (pciDeviceType != null) {
                deviceTypeStr = pciDeviceType.toString();
            } else {
                MdevDeviceType mdevDeviceType = Q.New(MdevDeviceVO.class)
                        .eq(MdevDeviceVO_.uuid, resourceUuid)
                        .select(MdevDeviceVO_.type)
                        .findValue();

                if (mdevDeviceType != null) {
                    deviceTypeStr = mdevDeviceType.toString();
                }
            }
            requiredSize = 1;
        }

        if (deviceTypeStr != null && quotaName.equals(getQuotaNameFromType(deviceTypeStr))) {
            return requiredSize;
        }

        return 0L;
    }

    private String getQuotaNameFromType(String deviceTypeString) {
        switch (deviceTypeString) {
            case PciDeviceConstants.GPU_Video_Controller:
            case PciDeviceConstants.GPU_3D_Controller:
                return PciDeviceConstants.GPU_NUMBER;
            case PciDeviceConstants.GPU_Audio_Controller:
            case PciDeviceConstants.GPU_USB_Controller:
            case PciDeviceConstants.GPU_Serial_Controller:
                break;
            default:
                return PciDeviceConstants.GENERIC_PCI_DEVICE_NUMBER;
        }

        return null;
    }

    @Override
    public void checkVmCapability(VmInstanceInventory inv, VmCapabilities capabilities) {
        if (!capabilities.isSupportLiveMigration()) {
            return;
        }

        if (Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.vmInstanceUuid, inv.getUuid())
                .isExists()) {
            capabilities.setSupportLiveMigration(false);
        }
    }

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {

    }

    private boolean isAttachableEthernetPciDevice(PciDeviceVO pci) {
        return pci.getType().equals(PciDeviceType.Ethernet_Controller) &&
                (Q.New(EthernetVfPciDeviceVO.class).eq(EthernetVfPciDeviceVO_.uuid, pci.getUuid()).isExists() ||
                        Q.New(VmVdpaNicVO.class).eq(VmVdpaNicVO_.pciDeviceUuid, pci.getUuid()).isExists());
    }

    private void attachPciDeviceToVmInstance(String vmUuid, String hostUuid, Completion completion) {
        List<PciDeviceVO> vos = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.vmInstanceUuid, vmUuid)
                .eq(PciDeviceVO_.hostUuid, hostUuid)
                .eq(PciDeviceVO_.status, PciDeviceStatus.Reserved)
                .list();
        
        for (PciDeviceVO pci : vos) {
            // skip Ethernet_Controller pci devices that are handled by VfNic/VdpaNic
            if (isAttachableEthernetPciDevice(pci)) {
                continue;
            }

            String pciUuid = pci.getUuid();
            if (!PciDeviceState.Enabled.equals(pci.getState())) {
                throw new OperationFailureException(operr(
                        "pci device[uuid:%s] doesn't exist or is disabled for vm[uuid:%s]", pciUuid, vmUuid));
            }

            if (PciDeviceUtils.getPciDeviceUuidsFromSameIommuGroup(pci.getUuid()).isEmpty()) {
                throw new OperationFailureException(operr(
                        PciDeviceUtils.iommuGroupError(PciDeviceInventory.valueOf(pci))
                ));
            }

            pci.setVmInstanceUuid(vmUuid);
            pci.setStatus(PciDeviceStatus.Attached); // Reserved -> Attached
            logger.debug(String.format(
                    "attached pci device[uuid:%s] choose by %s in iommu group[%s] of host[uuid:%s] to vm[uuid:%s]",
                    pci.getUuid(), pci.getChooser(), pci.getIommuGroup(), hostUuid, vmUuid));
        }

        if (vos.isEmpty()) {
            completion.success();
            return;
        }

        // mark pci devices attached
        dbf.updateCollection(vos);
        completion.success();

        /*
        // sync info of specified pci devices after attached to vm
        List<String> addresses = vos.stream().map(PciDeviceVO::getPciDeviceAddress).collect(Collectors.toList());

        SyncPciDeviceMsg smsg = new SyncPciDeviceMsg();
        smsg.setHostUuid(hostUuid);
        smsg.setPciDeviceAddresses(addresses);
        smsg.setHostIommuState(new HostIommuGetter().getState(hostUuid).toString());
        bus.makeTargetServiceIdByResourceUuid(smsg, PciDeviceConstants.SERVICE_ID, hostUuid);
        bus.send(smsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.debug(String.format("successfully synced info of pci devices[addrs:%s] from host[uuid:%s]", addresses, hostUuid));
                } else {
                    logger.error(String.format("failed to sync info of pci devices[addrs:%s] from host[uuid:%s]", addresses, hostUuid));
                }
                completion.success();
            }
        });
         */
    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        attachPciDeviceToVmInstance(spec.getVmInventory().getUuid(), spec.getDestHost().getUuid(), completion);
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        String vmUuid = spec.getVmInventory().getUuid();
        if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.NewCreate) {
            // release all pci devices if new created vm failed to start
            detachPciDevicesFromVm(spec.getVmInventory().getUuid(), true, true, true, completion);
        } else {
            // release spec related pci devices if already stopped vm failed to start
            detachPciDevicesFromVm(vmUuid, false,
                    PciDeviceUtils.releaseSpecReleatedPhysicalPciDevicesWhenStop(vmUuid),
                    PciDeviceUtils.releaseSpecReleatedVirtualPciDevicesWhenStop(vmUuid),
                    completion);
        }
    }

    @Override
    public void postBeforeInstantiateVmResource(VmInstanceSpec spec) {

    }

    private void cleanUpPciDeviceTagsOnVmInstance(String vmUuid) {
        // delete pciDeviceSpec tag after vm is started
        if (PciDeviceSystemTags.PCI_DEVICE_SPEC.hasTag(vmUuid)) {
            PciDeviceSystemTags.PCI_DEVICE_SPEC.delete(vmUuid);
        }
    }

    @Override
    public void postInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        String vmUuid = spec.getVmInventory().getUuid();
        cleanUpPciDeviceTagsOnVmInstance(vmUuid);
        completion.success();
    }

    @Override
    public void postReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }

    @Override
    public void releaseVmResource(VmInstanceSpec spec, Completion completion) {
        // if vm reboot, do nothing
        // if vm stopped, detach spec releated devices according to release configurations
        // if vm destroyed, detach spec releated devices by cascade, no matter what release configurations
        String vmUuid = spec.getVmInventory().getUuid();
        if (spec.getCurrentVmOperation() != VmInstanceConstant.VmOperation.Reboot) {
            detachPciDevicesFromVm(vmUuid, false,
                    PciDeviceUtils.releaseSpecReleatedPhysicalPciDevicesWhenStop(vmUuid),
                    PciDeviceUtils.releaseSpecReleatedVirtualPciDevicesWhenStop(vmUuid),
                    completion);
        } else {
            completion.success();
        }
    }

    /**
     * release different kinds of pci devices from vm
     * @param vmUuid
     * @param releaseNonSpec  release non-spec-releated pci devices
     * @param releasePhysicalSpec release physical-spec-releated pci devices
     * @param releaseVirtualSpec release virtual-spec-releated pci devices
     */
    private void detachPciDevicesFromVm(String vmUuid, boolean releaseNonSpec, boolean releasePhysicalSpec, boolean releaseVirtualSpec, Completion completion) {
        List<String> pciUuids = getNeedToDetachPciDevices(vmUuid, releaseNonSpec, releasePhysicalSpec, releaseVirtualSpec);
        if (pciUuids.isEmpty()) {
            completion.success();
            return;
        }

        logger.debug(String.format("going to detach pci devices from vm[uuid:%s], releaseNonSpec[%s], releasePhysicalSpec[%s], releaseVirtualSpec[%s]",
                vmUuid, releaseNonSpec, releasePhysicalSpec, releaseVirtualSpec));

        // call DetachPciDeviceFromVmMsg on above pci devices
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("detach-pci-devices");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(createDetachPciDeviceFromVmFlow(pciUuids));
                flow(createAttachPciDeviceToHostFlow(pciUuids));
                //flow(createSyncPciDeviceInfoFlow(pciUuids));

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        detachPciDeviceFromVmInDb(pciUuids);
                        logger.debug(String.format("detached pci devices[uuids:%s] from vm instance", pciUuids));
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        logger.error(String.format("failed to detach pci devices[uuids:%s] from vm instance: %s", pciUuids, errCode));
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Transactional(readOnly = true)
    public List<String> getNeedToDetachPciDevices(String vmUuid, boolean releaseNonSpec, boolean releasePhysicalSpec, boolean releaseVirtualSpec) {
        // get uuids of pci devices that are going to be released
        List<String> pciUuids = new ArrayList<>();

        if (!releaseNonSpec && !releasePhysicalSpec && !releaseVirtualSpec) {
            return pciUuids;
        }

        // get uuids of all pci devices attached to vmUuid
        List<String> allPcis = Q.New(PciDeviceVO.class)
                .orderBy(PciDeviceVO_.iommuGroup, SimpleQuery.Od.ASC)
                .orderBy(PciDeviceVO_.pciDeviceAddress, SimpleQuery.Od.ASC)
                .eq(PciDeviceVO_.vmInstanceUuid, vmUuid)
                .select(PciDeviceVO_.uuid)
                .listValues();

        if (allPcis.isEmpty()) {
            return pciUuids;
        }

        // pci devices associated to vf nics are not handled here
        List<String> vfNicPcis = Q.New(PciDeviceVO.class)
                .in(PciDeviceVO_.uuid, allPcis)
                .eq(PciDeviceVO_.type, PciDeviceType.Ethernet_Controller)
                .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUAL)
                .select(PciDeviceVO_.uuid)
                .listValues();
        allPcis.removeAll(vfNicPcis);

        if (allPcis.isEmpty()) {
            return pciUuids;
        }

        // get uuids of non-spec-released pci devices
        List<String> specPcis = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.vmInstanceUuid, vmUuid)
                .eq(PciDeviceVO_.chooser, PciDeviceChooser.Spec)
                .select(PciDeviceVO_.uuid)
                .listValues();
        for (String specPci : specPcis) {
            allPcis.removeAll(PciDeviceUtils.getPciDeviceUuidsFromSameIommuGroup(specPci));
        }

        if (releaseNonSpec && !allPcis.isEmpty()) {
            pciUuids.addAll(allPcis);
        }

        // get uuids of spec-related pci devices
        List<String> specUuids = Q.New(VmInstancePciDeviceSpecRefVO.class)
                .eq(VmInstancePciDeviceSpecRefVO_.vmInstanceUuid, vmUuid)
                .select(VmInstancePciDeviceSpecRefVO_.pciSpecUuid)
                .listValues();
        if (specUuids.isEmpty()) {
            return pciUuids;
        }

        List<String> physicalSpecs = Q.New(PciDeviceSpecVO.class)
                .eq(PciDeviceSpecVO_.isVirtual, false)
                .in(PciDeviceSpecVO_.uuid, specUuids)
                .select(PciDeviceSpecVO_.uuid)
                .listValues();

        List<String> virtualSpecs = Q.New(PciDeviceSpecVO.class)
                .eq(PciDeviceSpecVO_.isVirtual, true)
                .in(PciDeviceSpecVO_.uuid, specUuids)
                .select(PciDeviceSpecVO_.uuid)
                .listValues();

        if (releaseVirtualSpec && !virtualSpecs.isEmpty()) {
            List<String> virtualSpecPcis = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.vmInstanceUuid, vmUuid)
                    .eq(PciDeviceVO_.chooser, PciDeviceChooser.Spec)
                    .in(PciDeviceVO_.pciSpecUuid, virtualSpecs)
                    .select(PciDeviceVO_.uuid)
                    .listValues();
            for (String virtualSpecPci : virtualSpecPcis) {
                pciUuids.addAll(PciDeviceUtils.getPciDeviceUuidsFromSameIommuGroup(virtualSpecPci));
            }
        }

        if (releasePhysicalSpec && !physicalSpecs.isEmpty()) {
            List<String> physicalSpecPcis = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.vmInstanceUuid, vmUuid)
                    .eq(PciDeviceVO_.chooser, PciDeviceChooser.Spec)
                    .in(PciDeviceVO_.pciSpecUuid, physicalSpecs)
                    .select(PciDeviceVO_.uuid)
                    .listValues();
            for (String physicalSpecPci : physicalSpecPcis) {
                pciUuids.addAll(PciDeviceUtils.getPciDeviceUuidsFromSameIommuGroup(physicalSpecPci));
            }
        }

        return pciUuids;
    }

    @Override
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        // try to re-splite pci devices in host because sriov pci devices cannot persist when the host is rebooted
        // it will do before prepare l2, samrt nic will depend on it
        return new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                KVMHostInventory host = context.getInventory();
                List<PciDeviceVO> pcis = Q.New(PciDeviceVO.class)
                        .eq(PciDeviceVO_.hostUuid, host.getUuid())
                        .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED)
                        .list();
                if (pcis.isEmpty()) {
                    trigger.next();
                    return;
                }

                logger.debug(String.format("try to re-splite pci devices[uuid:%s] into sriov pci devices",
                        pcis.stream().map(PciDeviceVO::getUuid).collect(Collectors.toList())));
                new While<>(pcis).each((pci, comp) -> {
                    PciDeviceBackend bkd = getPciDeviceBackendByHostUuid(host.getUuid());
                    Integer virtPartNum = SQL.New("select spec.maxPartNum from PciDeviceVO pci, PciDeviceSpecVO spec " +
                            "where pci.parentUuid = :parentUuid and pci.pciSpecUuid = spec.uuid", Integer.class)
                            .param("parentUuid", pci.getUuid())
                            .limit(1)
                            .find();
                    if (virtPartNum == null) {
                        logger.warn(String.format("failed to get vf nums of virtualized pci device[uuid:%s]", pci.getUuid()));
                        comp.done();
                        return;
                    }

                    bkd.generateSriovPciDevices(pci.getHostUuid(), pci.toInventory(), virtPartNum, true, new Completion(comp) {
                        @Override
                        public void success() {
                            logger.debug(String.format("tried to re-splited pci device[uuid:%s] to sriov pci devices", pci.getUuid()));
                            comp.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.error(String.format("failed to re-splited pci device[uuid:%s] to sriov pci devices", pci.getUuid()));
                            comp.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        };
    }

    @Override
    public Flow createPostHostConnectFlow(HostInventory host) {
        return new NoRollbackFlow() {
            String __name__ = "sync-pci-devices";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                String hostUuid = host.getUuid();
                String hostIommu = new HostIommuGetter().getState(hostUuid).toString();
                SyncPciDeviceMsg smsg = new SyncPciDeviceMsg();
                smsg.setHostUuid(hostUuid);
                smsg.setHostIommuState(hostIommu);
                bus.makeTargetServiceIdByResourceUuid(smsg, PciDeviceConstants.SERVICE_ID, hostUuid);
                bus.send(smsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            SyncPciDeviceReply srly = reply.castReply();
                            if (!srly.isSuccess()) {
                                trigger.fail(srly.getError());
                            } else {
                                trigger.next();
                            }
                        }
                    }
                });
            }
        };
    }

    public Flow createAttachPciDeviceToVmFlow(List<String> pciUuids, String vmUuid) {
        return new Flow() {
            String __name__ = "attach-pci-device: virsh attach-device";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<ErrorCode> errs = new ArrayList<>();
                new While<>(pciUuids).each((pciUuid, compl) -> {
                    AttachPciDeviceToVmMsg amsg = new AttachPciDeviceToVmMsg();
                    amsg.setPciDeviceUuid(pciUuid);
                    amsg.setVmInstanceUuid(vmUuid);
                    bus.makeLocalServiceId(amsg, PciDeviceConstants.SERVICE_ID);
                    bus.send(amsg, new CloudBusCallBack(compl) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                logger.debug(String.format("attached pci device[uuid:%s] to vm[uuid:%s]", pciUuid, vmUuid));
                                compl.done();
                            } else {
                                errs.add(reply.getError());
                                compl.allDone();
                                logger.error(String.format("failed to attach pci device[uuid:%s] to vm[uuid:%s], %s", pciUuid, vmUuid, reply.getError()));
                            }
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errs.isEmpty()) {
                            trigger.fail(errs.get(0));
                        } else {
                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                FlowChain chain = FlowChainBuilder.newShareFlowChain();
                chain.setName(String.format("detach-pci-devices-of-same-group-as-%s-because-attach-rollbacked", pciUuids.get(0)));
                chain.then(new ShareFlow() {
                    @Override
                    public void setup() {
                        flow(createDetachPciDeviceFromVmFlow(pciUuids));
                        flow(createAttachPciDeviceToHostFlow(pciUuids));
                        //flow(createSyncPciDeviceInfoFlow(pciUuids));

                        done(new FlowDoneHandler(trigger) {
                            @Override
                            @Transactional
                            public void handle(Map data) {
                                detachPciDeviceFromVmInDb(pciUuids);
                                trigger.rollback();
                            }
                        });

                        error(new FlowErrorHandler(trigger) {
                            @Override
                            public void handle(ErrorCode errCode, Map data) {
                                trigger.rollback();
                            }
                        });
                    }
                }).start();
            }
        };
    }

    public Flow createDetachPciDeviceFromVmFlow(List<String> pciUuids) {
        List<PciDeviceInventory> invs = PciDeviceInventory.valueOf(
                Q.New(PciDeviceVO.class).in(PciDeviceVO_.uuid, pciUuids).notNull(PciDeviceVO_.vmInstanceUuid).list());

        return new NoRollbackFlow() {
            String __name__ = "detach-pci-device: virsh detach-device";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<ErrorCode> errs = new ArrayList<>();
                new While<>(invs).each((inv, compl) -> {
                    DetachPciDeviceFromVmMsg dmsg = new DetachPciDeviceFromVmMsg();
                    dmsg.setPciDeviceUuid(inv.getUuid());
                    dmsg.setVmInstanceUuid(inv.getVmInstanceUuid());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, PciDeviceConstants.SERVICE_ID, inv.getUuid());
                    bus.send(dmsg, new CloudBusCallBack(compl) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.error(String.format(
                                        "failed to detach pci device[uuid:%s] from vm[uuid:%s]: %s",
                                        inv.getUuid(), dmsg.getVmInstanceUuid(), reply.getError().toString()));
                                errs.add(reply.getError());
                                compl.allDone();
                            } else {
                                logger.debug(String.format("successfully detached pci device[uuid:%s] from vm[uuid:%s]",
                                        inv.getUuid(), dmsg.getVmInstanceUuid()));
                                compl.done();
                            }
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errs.isEmpty()) {
                            trigger.next();
                        } else {
                            trigger.fail(errs.get(0));
                        }
                    }
                });
            }
        };
    }

    public Flow createAttachPciDeviceToHostFlow(List<String> pciUuids) {
        List<PciDeviceInventory> invs = PciDeviceInventory.valueOf(Q.New(PciDeviceVO.class).in(PciDeviceVO_.uuid, pciUuids).list());

        return new NoRollbackFlow() {
            String __name__ = "detach-pci-device: virsh nodedev-reattach";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(invs).each((inv, compl) -> {
                    AttachPciDeviceToHostMsg rmsg = new AttachPciDeviceToHostMsg();
                    rmsg.setPciDeviceUuid(inv.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(rmsg, PciDeviceConstants.SERVICE_ID, inv.getUuid());
                    bus.send(rmsg, new CloudBusCallBack(compl) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(reply.getError().toString());
                            } else {
                                logger.debug(String.format("successfully reattached pci device[uuid:%s] to host", inv.getUuid()));
                            }
                            compl.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        };
    }

    /**
     * Simple version of PciDeviceReserveFlow.
     * Make sure the target pci device still available, then reserve it.
     * @param pciUuid the pci device that user chose to attach to vm
     * @return a flow
     */
    private Flow checkAndReservePciDeviceFlow(String vmUuid, String pciUuid) {
        return new Flow() {
            String __name__ = "check-and-reserve-pci-device-before-attach-to-vm";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                CheckAndReservePciDeviceMsg cmsg = new CheckAndReservePciDeviceMsg();
                cmsg.setVmUuid(vmUuid);
                cmsg.setPciUuid(pciUuid);
                bus.makeTargetServiceIdByResourceUuid(cmsg, PciDeviceConstants.SERVICE_ID, pciUuid);
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            CheckAndReservePciDeviceReply rly = reply.castReply();
                            if (!rly.isSuccess()) {
                                trigger.fail(rly.getError());
                            } else {
                                data.put("reserved-pci-devices", rly.getReservedPciDevices());
                                trigger.next();
                            }
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                List<String> reservedPciDevices = (List<String>) data.get("reserved-pci-devices");
                if (CollectionUtils.isEmpty(reservedPciDevices)) {
                    trigger.rollback();
                    return;
                }

                PciDeviceUtils.cleanUpReserveTags(vmUuid, reservedPciDevices);
                trigger.rollback();
            }
        };
    }

    public Flow createDetachPciDeviceFromHostFlow(List<String> pciUuids) {
        List<PciDeviceInventory> invs = PciDeviceInventory.valueOf(Q.New(PciDeviceVO.class).in(PciDeviceVO_.uuid, pciUuids).list());

        return new NoRollbackFlow() {
            String __name__ = "attach-pci-device: virsh nodedev-detach";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(invs).all((inv, compl) -> {
                    DetachPciDeviceFromHostMsg dmsg = new DetachPciDeviceFromHostMsg();
                    dmsg.setPciDeviceUuid(inv.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, PciDeviceConstants.SERVICE_ID, inv.getUuid());
                    bus.send(dmsg, new CloudBusCallBack(compl) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(reply.getError().toString());
                            } else {
                                logger.debug(String.format("successfully detached pci device[uuid:%s] from host", inv.getUuid()));
                            }
                            compl.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        };
    }

    public Flow createSyncPciDeviceInfoFlow(List<String> pciUuids) {
        return new NoRollbackFlow() {
            String __name__ = "sync pci device infos after attach/detach";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<PciDeviceVO> pcis = dbf.listByPrimaryKeys(pciUuids, PciDeviceVO.class);
                if (pcis == null || pcis.isEmpty() || 1 != pcis.stream().map(PciDeviceVO::getHostUuid).distinct().count()) {
                    trigger.next();
                    return;
                }

                String hostUuid = pcis.get(0).getHostUuid();
                List<String> addresses = pcis.stream().map(PciDeviceVO::getPciDeviceAddress).collect(Collectors.toList());

                SyncPciDeviceMsg smsg = new SyncPciDeviceMsg();
                smsg.setHostUuid(hostUuid);
                smsg.setPciDeviceAddresses(addresses);
                smsg.setHostIommuState(new HostIommuGetter().getState(hostUuid).toString());
                bus.makeTargetServiceIdByResourceUuid(smsg, PciDeviceConstants.SERVICE_ID, hostUuid);
                bus.send(smsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            logger.debug(String.format("successfully synced info of pci devices[uuids:%s] from host[uuid:%s]", pciUuids, hostUuid));
                        } else {
                            logger.error(String.format("failed to sync info of pci devices[uuids:%s] from host[uuid:%s]", pciUuids, hostUuid));
                        }
                        trigger.next();
                    }
                });
            }
        };
    }

    @Transactional
    public void detachPciDeviceFromVmInDb(List<String> pciUuids) {
        for (String pciUuid : pciUuids) {
            PciDeviceVO pci = dbf.findByUuid(pciUuid, PciDeviceVO.class);
            String vmInstanceUuid = pci.getVmInstanceUuid();
            if (vmInstanceUuid == null) {
                continue;
            }

            // update pci device status
            pci.setStatus(PciDeviceUtils.getAvailableStatus(pciUuid));
            pci.setVmInstanceUuid(null);
            pci.setChooser(PciDeviceChooser.None);
            dbf.update(pci);

            // broadcast pci device status changed
            PciDeviceCanonicalEvents.PciDeviceStateChangedData cdata = new PciDeviceCanonicalEvents.PciDeviceStateChangedData();
            cdata.setVmUuid(vmInstanceUuid);
            cdata.setPciDeviceUuid(pciUuid);
            cdata.setHostUuid(pci.getHostUuid());
            cdata.setStatus(pci.getStatus().toString());
            cdata.setInventory(PciDeviceInventory.valueOf(pci));
            cdata.setDescription(pci.getDescription());
            evtf.fire(PciDeviceCanonicalEvents.PCIDEVICE_FULL_STATE_CHANGED_PATH, cdata);
        }
    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        attachPciDeviceToVmInstance(inv.getUuid(), destHostUuid, new Completion(null) {
            @Override
            public void success() {
                logger.debug(String.format("attached pci device to vm[uuid:%s] after migration", inv.getUuid()));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("failed to attach pci device to vm[uuid:%s] after migration", inv.getUuid()));
            }
        });
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        cleanUpPciDeviceTagsOnVmInstance(inv.getUuid());
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        // release pci devices if vm failed to migrate
        detachPciDevicesFromVm(inv.getUuid(), true, true, true, new Completion(null) {
            @Override
            public void success() {
                logger.debug(String.format("auto detached pci devices from vm[uuid:%s] because it failed to migrate", inv.getUuid()));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("failed to auto detach pci devices from vm[uuid:%s] because it's failed to migrate", inv.getUuid()));
            }
        });
    }

    @Override
    @Transactional
    public boolean canDoVmHa(String vmUuid) {
        // release pci devices before check
        VmInstanceState state = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).select(VmInstanceVO_.state).findValue();
        if (state == VmInstanceState.Stopped || state == VmInstanceState.Unknown) {
            detachPciDeviceFromVmInDb(getNeedToDetachPciDevices(vmUuid, false,
                    PciDeviceUtils.releaseSpecReleatedPhysicalPciDevicesWhenStop(vmUuid),
                    PciDeviceUtils.releaseSpecReleatedVirtualPciDevicesWhenStop(vmUuid)
            ));
        }

        // do the check
        String hostUuid = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.status, PciDeviceStatus.Attached)
                .eq(PciDeviceVO_.vmInstanceUuid, vmUuid)
                .notEq(PciDeviceVO_.type, PciDeviceType.Ethernet_Controller)
                .select(PciDeviceVO_.hostUuid)
                .limit(1)
                .findValue();
        if (StringUtils.isEmpty(hostUuid)) {
            return true;
        }

        // the vm can only start in the host where the pci device on
        return Q.New(HostVO.class)
                .eq(HostVO_.uuid, hostUuid)
                .eq(HostVO_.state, HostState.Enabled)
                .eq(HostVO_.status, HostStatus.Connected)
                .isExists();
    }

    @Override
    public Flow createVmAbnormalLifeCycleHandlingFlow(VmAbnormalLifeCycleStruct struct) {
        return new Flow() {
            String __name__ = "auto-release-attached-pci-devices";

            VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation operation = struct.getOperation();
            VmInstanceInventory vm = struct.getVmInstance();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (operation == VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation.VmStoppedOnTheSameHost ||
                        operation == VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation.VmStoppedFromPausedStateHostNotChanged) {
                    vmStoppedOnTheSameHost(trigger);
                } else {
                    trigger.next();
                }
            }

            private void vmStoppedOnTheSameHost(final FlowTrigger trigger) {
                String vmUuid = vm.getUuid();
                detachPciDevicesFromVm(vm.getUuid(), false,
                        PciDeviceUtils.releaseSpecReleatedPhysicalPciDevicesWhenStop(vmUuid),
                        PciDeviceUtils.releaseSpecReleatedVirtualPciDevicesWhenStop(vmUuid),
                        new Completion(null) {
                            @Override
                            public void success() {
                                logger.debug(String.format("auto detached pci devices from vm[uuid:%s] because it's been stopped bypass zstack", vmUuid));
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.error(String.format("failed to auto detach pci devices from vm[uuid:%s] because it's been stopped bypass zstack", vmUuid));
                                trigger.next();
                            }
                        });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
            }
        };
    }

    // TODO: reduce rom creation frequency
    // create pci device rom file in dest host if rom content not empty
    public void downloadPciDeviceRomToHostForVm(String hostUuid, String vmUuid) {
        Map<String, Integer> specMap = PciDeviceUtils.getVmPciSpecUuids(vmUuid);
        PciDeviceBackend bkd = getPciDeviceBackendByHostUuid(hostUuid);
        new While<>(specMap.keySet()).all((uuid, compl) -> {
            PciDeviceSpecVO pciSpec = dbf.findByUuid(uuid, PciDeviceSpecVO.class);
            boolean abandonSpecRom = StringUtils.isEmpty(pciSpec.getRomContent());
            bkd.createPciDeviceRomFileInHost(pciSpec.toInventory(), hostUuid, new Completion(null) {
                @Override
                public void success() {
                    logger.debug(String.format("successfully %s pci device rom file[name:%s] in host[uuid:%s]",
                            abandonSpecRom ? "deleted" : "created",
                            pciSpec.getUuid(), hostUuid));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.error(String.format("failed to %s pci device rom file[name:%s] in host[uuid:%s]",
                            abandonSpecRom ? "delete" : "create",
                            pciSpec.getUuid(), hostUuid));
                }
            });
        }).run(new NopeWhileDoneCompletion());
    }

    @Override
    public List<HostNetworkBondingVO> filterBondingCandidates(List<HostNetworkBondingVO> candidates, List<String> hostUuids) {
        return candidates;
    }

    @Override
    public List<HostNetworkInterfaceVO> filterInterfaceCandidates(List<HostNetworkInterfaceVO> candidates, List<String> hostUuids) {
        if (CollectionUtils.isEmpty(hostUuids) || CollectionUtils.isEmpty(candidates)) {
            return candidates;
        }

        List<String> excludedUuids = SQL.New("select distinct if.uuid from PciDeviceVO pci, HostNetworkInterfaceVO if" +
                        " where pci.pciDeviceAddress = if.pciDeviceAddress" +
                        " and pci.hostUuid = if.hostUuid" +
                        " and pci.passThroughState = :passThroughState" +
                        " and pci.hostUuid in (:hostUuids)" +
                        " and if.uuid in (:interfaceUuids)")
                .param("passThroughState", PciDevicePassThroughState.Enabled)
                .param("hostUuids", hostUuids)
                .param("interfaceUuids", candidates.stream().map(HostNetworkInterfaceVO::getUuid).collect(Collectors.toList()))
                .list();

        return candidates.stream().filter(it -> !excludedUuids.contains(it.getUuid())).collect(Collectors.toList());
    }
}

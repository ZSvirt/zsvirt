package org.zstack.compute.vdpa;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.cluster.MevocoClusterGlobalConfig;
import org.zstack.compute.host.*;
import org.zstack.compute.ovs.OvsGlobalConfig;
import org.zstack.compute.sriov.VfPciDeviceUtils;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.allocator.HostAllocatorError;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.sriov.*;
import org.zstack.header.vdpa.*;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.*;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.pciDevice.PciDeviceInventory;
import org.zstack.pciDevice.PciDeviceVO;
import org.zstack.pciDevice.PciDeviceVO_;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by haibiao.xiao on 4/9/2021
 */
public class VmVdpaNicManagerImpl extends AbstractService implements VmVdpaNicManager,
        KvmPreAttachNicExtensionPoint,
        KvmPreDetachNicExtensionPoint,
        ReleaseNetworkServiceOnDetachingNicExtensionPoint,
        PreVmInstantiateResourceExtensionPoint,
        KVMStartVmExtensionPoint,
        VmInstanceMigrateExtensionPoint,
        KVMHostConnectExtensionPoint,
        PostHostConnectExtensionPoint,
        KvmHostAgentDeploymentExtensionPoint{

    private static final CLogger logger = Utils.getLogger(VmVdpaNicManagerImpl.class);
    private Map<String, VmVdpaNicHypervisorBackend> vmVdpaNicHypervisorBackend = new HashMap<>();

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    @Qualifier("KVMHostFactory")
    protected KVMHostFactory factory;

    private static final VfPciDeviceUtils vfPciDeviceUtils = new VfPciDeviceUtils();

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void populateExtensions() {
        for (VmVdpaNicHypervisorBackend bkd : pluginRgty.getExtensionList(VmVdpaNicHypervisorBackend.class)) {
            String type = bkd.getHypervisorType().toString();
            VmVdpaNicHypervisorBackend old = vmVdpaNicHypervisorBackend.get(type);
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VmVdpaNicHypervisorBackend[%s, %s] for type[%s]",
                        bkd.getClass().getName(), old.getClass().getName(), type));
            }
            vmVdpaNicHypervisorBackend.put(type, bkd);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof DeleteVdpasMsg) {
            handle((DeleteVdpasMsg) msg);
        } else if (msg instanceof GenerateVdpaMsg) {
            handle((GenerateVdpaMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private String getSyncId(String vmUuid) {
        return String.format("vdpa-operation-for-vm-%s", vmUuid);
    }

    private void handle(DeleteVdpasMsg msg) {
        String hypervisorType = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.hypervisorType)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .findValue();

        String hostUuid = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.lastHostUuid)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .findValue();

        final DeleteVdpasReply reply = new DeleteVdpasReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId(msg.getVmInstanceUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                VmVdpaNicHypervisorBackend bkd = vmVdpaNicHypervisorBackend.get(hypervisorType);
                bkd.expungeVdpas(hostUuid, msg, new Completion(msg) {
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
                return DeleteVdpasMsg.class.getName();
            }
        });
    }

    private void handle(GenerateVdpaMsg msg) {
        String hypervisorType = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.hypervisorType)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .findValue();

        String hostUuid = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.lastHostUuid)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .findValue();

        final GenerateVdpaReply reply = new GenerateVdpaReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId(msg.getVmInstanceUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                VmVdpaNicHypervisorBackend bkd = vmVdpaNicHypervisorBackend.get(hypervisorType);
                bkd.generateVdpa(hostUuid, msg, new Completion(msg) {
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
                return GenerateVdpaMsg.class.getName();
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VmVdpaNicConstant.SERVICE_ID);
    }

    /**
     * Attach Nic,  prepare AttachNicCommand using pci device address & src path in VmVdpaNicVO.
     * @param host host inventory
     * @param cmd attach nic command
     */
    @Override
    public void preAttachNicExtensionPoint(KVMHostInventory host, KVMAgentCommands.AttachNicCommand cmd) {
        /*
         * Update vDPA nic src path and pci device address. in AttachNicCommand.
         */
        String nicUuid = cmd.getNic().getUuid();

        VmVdpaNicVO vdpa = dbf.findByUuid(nicUuid, VmVdpaNicVO.class);
        if (vdpa == null) {
            return;
        }

        String pciAddress = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.uuid, vdpa.getPciDeviceUuid())
                .select(PciDeviceVO_.pciDeviceAddress)
                .findValue();
        cmd.getNic().setPciDeviceAddress(pciAddress);

        cmd.getNic().setSrcPath(vdpa.getSrcPath());

        logger.debug(String.format("updated vDPA nic src path in attachNicCmd for vm[uuid:%s]", cmd.getVmUuid()));
    }

    /**
     * Detach Nic, prepare DetachNicCommand using pci device address & src path in VmVdpaNicVO.
     * @param host host inventory
     * @param cmd detach nic command
     */
    @Override
    public void preDetachNicExtensionPoint(KVMHostInventory host, KVMAgentCommands.DetachNicCommand cmd) {
        /*
         * Update vDPA nic src path and pci device address. in DetachNicCommand.
         */
        String nicUuid = cmd.getNic().getUuid();

        VmVdpaNicVO vdpa = dbf.findByUuid(nicUuid, VmVdpaNicVO.class);
        if (vdpa == null) {
            return;
        }

        String pciAddress = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.uuid, vdpa.getPciDeviceUuid())
                .select(PciDeviceVO_.pciDeviceAddress)
                .findValue();
        cmd.getNic().setPciDeviceAddress(pciAddress);

        cmd.getNic().setSrcPath(vdpa.getSrcPath());

        logger.debug(String.format("updated vDPA nic src path in detachNicCmd for vm[uuid:%s]", cmd.getVmUuid()));
    }

    @Override
    public void releaseResourceOnDetachingNic(VmInstanceSpec spec, VmNicInventory nic, NoErrorCompletion completion) {
        if (!nic.getType().equals(VmVdpaNicConstant.VIRTIO_DATA_PATH_ACCEL_TYPE)) {
            completion.done();
            return;
        }

        vfPciDeviceUtils.releaseVfDevice(nic);
        completion.done();
    }

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {

    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        /*
         * Allocate the reserved vf nic pci devices to the starting vm.
         */
        String vmUuid = spec.getVmInventory().getUuid();
        String hostUuid = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.hostUuid).findValue();
        List<VmVdpaNicVO> vdpaNicVOS = Q.New(VmVdpaNicVO.class).eq(VmVfNicVO_.vmInstanceUuid, vmUuid).list();
        if (vdpaNicVOS.isEmpty()) {
            completion.success();
            return;
        }

        ReserveEthernetVfMsg msg = new ReserveEthernetVfMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmUuid(vmUuid);
        msg.setL3Uuids(vdpaNicVOS.stream().map(VmVdpaNicVO::getL3NetworkUuid).collect(Collectors.toList()));
        msg.setReleaseOldVf(true);
        msg.setStatus(EthernetVfStatus.Attached);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                ReserveHostPciDeviceReply r = reply.castReply();
                for (PciDeviceInventory pci : r.getPciDevices()) {
                    EthernetVfPciDeviceVO vfPciDeviceVO = dbf.findByUuid(pci.getUuid(), EthernetVfPciDeviceVO.class);

                    for (VmVdpaNicVO vfNic : vdpaNicVOS) {
                        if (vfPciDeviceVO.getL3NetworkUuid().equals(vfNic.getL3NetworkUuid())) {
                            logger.debug(String.format("set vm[name:%s] nic [name:%s, l3Uuid:%s] vf pci address[:%s]]",
                                    spec.getVmInventory().getName(), vfNic.getInternalName(), vfNic.getL3NetworkUuid(),
                                    vfPciDeviceVO.getPciDeviceAddress()));
                            vfNic.setPciDeviceUuid(vfPciDeviceVO.getUuid());
                        }
                    }
                }

                dbf.updateCollection(vdpaNicVOS);

                completion.success();
            }
        });
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }

    /**
     * CreateVmOnHypervisor StartVmOnHypervisor. prepare nicTO using pci device address & src path in VmVdpaNicVO.
     * @param host
     * @param spec
     * @param cmd
     */
    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        String vmUuid = spec.getVmInventory().getUuid();
        List<String> vdpaNicUuids = Q.New(VmVdpaNicVO.class)
                .eq(VmVdpaNicVO_.vmInstanceUuid, vmUuid)
                .select(VmVdpaNicVO_.uuid).listValues();

        if (vdpaNicUuids.isEmpty()) {
            return;
        }

        for (String vdpaNicUuid: vdpaNicUuids) {
            Optional<KVMAgentCommands.NicTO> nic = cmd.getNics().stream().filter(n -> n.getUuid().equals(vdpaNicUuid)).findFirst();
            if (!nic.isPresent()) {
                logger.error(String.format("vm[uuid:%s] has vnic[uuid:%s] but it doesn't appears in the start vm cmd %s",
                        vmUuid, vdpaNicUuid, cmd));
            } else {
                KVMAgentCommands.NicTO nicTO = nic.get();

                VmVdpaNicVO vdpVO = dbf.findByUuid(vdpaNicUuid, VmVdpaNicVO.class);
                DebugUtils.Assert(vdpVO != null, String.format("vf nic[uuid:%s] doesn't exists", vdpaNicUuid));

                String pciAddress = Q.New(PciDeviceVO.class).eq(PciDeviceVO_.uuid, vdpVO.getPciDeviceUuid())
                        .select(PciDeviceVO_.pciDeviceAddress)
                        .findValue();
                nicTO.setPciDeviceAddress(pciAddress);
                nicTO.setSrcPath(vdpVO.getSrcPath());
            }
        }

        logger.debug(String.format("updated vdpa nic pci device address in startVmCmd for vm[uuid:%s]", vmUuid));
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {
        String vmUuid = spec.getVmInventory().getUuid();
        List<String> vdpaNicUuids = Q.New(VmVdpaNicVO.class)
                .eq(VmVdpaNicVO_.vmInstanceUuid, vmUuid)
                .select(VmVdpaNicVO_.uuid).listValues();

        if (vdpaNicUuids.isEmpty()) {
            return;
        }

        DeleteVdpasMsg msg = new DeleteVdpasMsg();
        msg.setVmInstanceUuid(vmUuid);
        VmVdpaNicHypervisorBackend bkd = vmVdpaNicHypervisorBackend.get(host.getHypervisorType());
        bkd.expungeVdpas(host.getUuid(), msg, new Completion(msg) {
            @Override
            public void success() {
                logger.debug(String.format("expunge vdpa nic resource for vm[uuid:%s] in host[uuid:%s]", vmUuid, host.getUuid()));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to expunge vdpa nic resource for vm[uuid:%s] in host[uuid:%s]", vmUuid, host.getUuid()));
            }
        });
    }

    private List<KVMAgentCommands.NicTO> getNicTos(String vmUuid) {
        List<VmVdpaNicVO> nics = Q.New(VmVdpaNicVO.class).eq(VmVdpaNicVO_.vmInstanceUuid, vmUuid)
                .eq(VmVdpaNicVO_.type, VmVdpaNicConstant.VIRTIO_DATA_PATH_ACCEL_TYPE)
                .list();
        return VmVdpaNicInventory.valueOf(nics).stream().map(this::completeVdpaNicInfo).collect(Collectors.toList());
    }

    private List<String> getCleanTrafficIp(VmNicInventory nic) {
        boolean isUserVm = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, nic.getVmInstanceUuid()).select(VmInstanceVO_.type)
                .findValue().equals(VmInstanceConstant.USER_VM_TYPE);

        if (!isUserVm) {
            return null;
        }

        String tagValue = VmSystemTags.CLEAN_TRAFFIC.getTokenByResourceUuid(nic.getVmInstanceUuid(), VmSystemTags.CLEAN_TRAFFIC_TOKEN);
        if (Boolean.parseBoolean(tagValue) || (tagValue == null && VmGlobalConfig.VM_CLEAN_TRAFFIC.value(Boolean.class))) {
            return VmNicHelper.getIpAddresses(nic);
        }

        return null;
    }

    private L2NetworkInventory getL2NetworkTypeFromL3NetworkUuid(String l3NetworkUuid) {
        String sql = "select l2 from L2NetworkVO l2 where l2.uuid = (select l3.l2NetworkUuid from L3NetworkVO l3 where l3.uuid = :l3NetworkUuid)";
        TypedQuery<L2NetworkVO> query = dbf.getEntityManager().createQuery(sql, L2NetworkVO.class);
        query.setParameter("l3NetworkUuid", l3NetworkUuid);
        L2NetworkVO l2vo = query.getSingleResult();
        return L2NetworkInventory.valueOf(l2vo);
    }

    @Transactional(readOnly = true)
    private KVMAgentCommands.NicTO completeVdpaNicInfo(VmVdpaNicInventory nic) {
        L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class));
        L2NetworkInventory l2inv = getL2NetworkTypeFromL3NetworkUuid(nic.getL3NetworkUuid());
        KVMCompleteNicInformationExtensionPoint extp = factory.getCompleteNicInfoExtension(L2NetworkType.valueOf(l2inv.getType()));
        KVMAgentCommands.NicTO to = extp.completeNicInformation(l2inv, l3Inv, nic);

        if (to.getUseVirtio() == null) {
            to.setUseVirtio(VmSystemTags.VIRTIO.hasTag(nic.getVmInstanceUuid()));
            to.setIps(getCleanTrafficIp(nic));
        }

        if (nic.getType().equals(VmVdpaNicConstant.VIRTIO_DATA_PATH_ACCEL_TYPE)) {
            String pciAddress = Q.New(PciDeviceVO.class).eq(PciDeviceVO_.uuid, nic.getPciDeviceUuid())
                    .select(PciDeviceVO_.pciDeviceAddress).findValue();
            to.setPciDeviceAddress(pciAddress);
            to.setSrcPath(nic.getSrcPath());
        }

        KVMAgentCommands.VHostAddOn vHostAddOn = new KVMAgentCommands.VHostAddOn();

        vHostAddOn.setQueueNum(rcf.getResourceConfigValue(VmGlobalConfig.VM_NIC_MULTIQUEUE_NUM, nic.getVmInstanceUuid(), Integer.class));
        if (VmSystemTags.VM_VRING_BUFFER_SIZE.hasTag(nic.getVmInstanceUuid())) {
            Map<String, String> tokens = VmSystemTags.VM_VRING_BUFFER_SIZE.getTokensByResourceUuid(nic.getVmInstanceUuid());
            if (tokens.get(VmSystemTags.RX_SIZE_TOKEN) != null) {
                vHostAddOn.setRxBufferSize(tokens.get(VmSystemTags.RX_SIZE_TOKEN));
            }

            if (tokens.get(VmSystemTags.TX_SIZE_TOKEN) != null) {
                vHostAddOn.setTxBufferSize(tokens.get(VmSystemTags.TX_SIZE_TOKEN));
            }
        }

        to.setvHostAddOn(vHostAddOn);

        return to;
    }

    private void genVdpas(VmInstanceInventory inv, String destHostUuid, NoErrorCompletion completion) {
        List<KVMAgentCommands.NicTO> nicTos = getNicTos(inv.getUuid());
        if (nicTos.isEmpty()) {
            return;
        }
        GenerateVdpaMsg msg = new GenerateVdpaMsg();
        msg.setVmInstanceUuid(inv.getUuid());
        msg.setNics(nicTos);
        VmVdpaNicHypervisorBackend bkd = vmVdpaNicHypervisorBackend.get(inv.getHypervisorType());

        bkd.generateVdpa(destHostUuid, msg, new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.done();
            }
        });
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        if (StringUtils.equals(inv.getHostUuid(), destHostUuid)) {
            return;
        }

        List<VmNicInventory> vmNics = inv.getVmNics().stream()
                .filter(vn -> vn.getType().equals(VmVdpaNicConstant.VIRTIO_DATA_PATH_ACCEL_TYPE))
                .collect(Collectors.toList());

        if (vmNics.isEmpty()) {
            return;
        }

        FutureCompletion completion = new FutureCompletion(null);
        genVdpas(inv, destHostUuid, new NoErrorCompletion() {
            @Override
            public void done() {
                completion.success();
            }
        });
        completion.await(TimeUnit.MINUTES.toMillis(3));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(operr("cannot generate vdpa for vm[uuid:%s] on the destination host[uuid:%s]",
                    inv.getUuid(), destHostUuid).causedBy(completion.getErrorCode()));
        }
    }

    private void releaseUselessPciOnHost(VmInstanceInventory inv, String hostUuid, NoErrorCompletion completion) {
        DeleteVdpasMsg msg = new DeleteVdpasMsg();
        msg.setVmInstanceUuid(inv.getUuid());
        VmVdpaNicHypervisorBackend bkd = vmVdpaNicHypervisorBackend.get(inv.getHypervisorType());
        bkd.expungeVdpas(hostUuid, msg, new Completion(completion) {
            @Override
            public void success() {
                logger.debug(String.format("release vm [uuid:%s] vdpa on host[uuid:%s] success",
                        inv.getUuid(), hostUuid));
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to release vdpa nic for vm[uuid:%s] after migrate : %s",
                        inv.getUuid(), errorCode));
                completion.done();
            }
        });
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        List<VmNicInventory> vdpaNics = inv.getVmNics().stream().filter(nic ->
                nic.getType().equals(VmVdpaNicConstant.VIRTIO_DATA_PATH_ACCEL_TYPE)).collect(Collectors.toList());
        if (vdpaNics.isEmpty()) {
            return;
        }

        for (VmNicInventory nic : vdpaNics) {
            vfPciDeviceUtils.allocateReservedVfDevice(nic);
            vfPciDeviceUtils.releaseToBeReleasedVfPciDevice(nic);
        }

        FutureCompletion completion = new FutureCompletion(null);
        releaseUselessPciOnHost(inv, srcHostUuid, new NoErrorCompletion(completion) {
            @Override
            public void done() {
                completion.success();
            }
        });
        completion.await(TimeUnit.MINUTES.toMillis(3));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(operr("release vdpa for vm[uuid:%s] on the destination host[uuid:%s]",
                    inv.getUuid(), srcHostUuid).causedBy(completion.getErrorCode()));
        }
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        //if no allocate host
        if (HostAllocatorError.NO_AVAILABLE_HOST.toString().equals(reason.getCode())) {
            logger.debug(String.format("no need to release vdpa and pci for vm[uuid:%s]",inv.getUuid()));
            return;
        }

        List<VmNicInventory> vdpaNics = inv.getVmNics().stream()
                .filter(nic -> nic.getType().equals(VmVdpaNicConstant.VIRTIO_DATA_PATH_ACCEL_TYPE))
                .collect(Collectors.toList());
        if (vdpaNics.isEmpty()) {
            logger.debug(String.format("no vdpa nic for vm[uuid:%s]",inv.getUuid()));
            return;
        }

        FutureCompletion completion = new FutureCompletion(null);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("restore-vm-vpda-nic-%s", inv.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = String.format("restore-vdpa-on-old-host");

            @Override
            public void run(FlowTrigger trigger, Map data) {
                genVdpas(inv, inv.getHostUuid(), new NoErrorCompletion() {
                    @Override
                    public void done() {
                        for (VmNicInventory nic : vdpaNics) {
                            vfPciDeviceUtils.restoreVfPciDevice(nic);
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("delete-vdpa-on-new-host");

            @Override
            public void run(FlowTrigger trigger, Map data) {
                releaseUselessPciOnHost(inv, destHostUuid, new NoErrorCompletion() {
                    @Override
                    public void done() {
                        vfPciDeviceUtils.releaseVfDevice(inv);
                        trigger.next();
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
        completion.await(TimeUnit.MINUTES.toMillis(3));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(operr("restore vdpa for vm[uuid:%s] from the destination host[uuid:%s]",
                    inv.getUuid(), destHostUuid).causedBy(completion.getErrorCode()));
        }
    }

    @Override
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        String __name__ = String.format("preconfigure-ovsdpdk-host-%s", context.getInventory().getUuid());
        return new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                String clusterUuid = context.getInventory().getClusterUuid();
                Boolean isOvsSup = rcf.getResourceConfigValue(MevocoClusterGlobalConfig.OVS_DPDK_SUPPORT, clusterUuid, Boolean.class);
                if (!isOvsSup) {
                    trigger.next();
                    return;
                }

                KVMHostInventory host = context.getInventory();
                String hostUuid = host.getUuid();

                String ovsReservedMemoryConfig = rcf.getResourceConfigValue(OvsGlobalConfig.RESERVED_MEMORY_CAPACITY_FOR_OVSDPDK, clusterUuid, String.class);
                long ovsReservedMemory = SizeUtils.sizeStringToBytes(ovsReservedMemoryConfig);
                long reservMInKb = SizeUnit.BYTE.toKiloByte(ovsReservedMemory);
                long reservM = SizeUnit.BYTE.toMegaByte(ovsReservedMemory);

                Integer hugePageSizeForOvsDpdkConfig = rcf.getResourceConfigValue(OvsGlobalConfig.HUGEPAGE_SIZE_FOR_OVSDPDK, clusterUuid, Integer.class);
                long hugePageSizeForOvsDpdk = SizeUnit.MEGABYTE.toKiloByte(hugePageSizeForOvsDpdkConfig);

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                MevocoKVMAgentCommands.PreconfigureOvsDpdkCmd cmd = new MevocoKVMAgentCommands.PreconfigureOvsDpdkCmd();
                cmd.setPageSize(hugePageSizeForOvsDpdk);
                cmd.setReserveSize(reservMInKb);
                cmd.setSocketMem((int)(reservM * VmVdpaNicConstant.OVS_DPDK_MEM_USAGE_PRECENT));

                msg.setNoStatusCheck(true);
                msg.setCommand(cmd);
                msg.setPath(MevocoKVMConstant.PRECONFIGURE_OVSDPDK_PATH);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
                msg.setHostUuid(host.getUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.debug(String.format("failed to preconfigure ovsdpdk in host[uuid:%s]: %s", hostUuid, reply.getError()));
                        } else {
                            logger.debug(String.format("preconfigure ovsdpdk in host[uuid:%s] successfully", hostUuid));
                        }
                        trigger.next();
                    }
                });
            }
        };

    }

    @Override
    public Flow createPostHostConnectFlow(HostInventory host) {

        String __name__ = String.format("sync-vdpa-nic-to-host-%s", host.getUuid());
        return new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                String clusterUuid = host.getClusterUuid();
                Boolean isOvsSup = rcf.getResourceConfigValue(MevocoClusterGlobalConfig.OVS_DPDK_SUPPORT, clusterUuid, Boolean.class);
                if (!isOvsSup) {
                    trigger.next();
                    return;
                }
                boolean enableVhostUser = NetworkServiceGlobalConfig.ENABLE_VHOSTUSER.value(Boolean.class);
                if (enableVhostUser) {
                    trigger.next();
                    return;
                }
                /*
                 * 1.get vdpanic
                 * 2.sync vpd
                 * */
                List<MevocoKVMAgentCommands.OvsDpdkBridgeTO> vdpaNicInfo = getVdpaNicInfoByHost(host);
                if (vdpaNicInfo.isEmpty()) {
                    logger.debug(String.format("sync empty vdpa nic to host[uuid:%s, name:%s] successfully",
                            host.getUuid(), host.getName()));
                }

                Map<String, List<MevocoKVMAgentCommands.OvsDpdkBridgeTO>> hostMap = new HashMap<>();
                hostMap.put(host.getUuid(), vdpaNicInfo);

                syncVdpaToHost(hostMap, new Completion(trigger) {
                    @Override
                    public void success() {
                        logger.debug(String.format("sync vdpa to host[uuid:%s, name:%s] successfully",
                                host.getUuid(), host.getName()));
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.debug(String.format("sync vdpa to host[uuid:%s, name:%s] successfully",
                                host.getUuid(), host.getName()));
                        trigger.next();
                    }
                });
            }
        };
    }

    private List<MevocoKVMAgentCommands.OvsDpdkBridgeTO> getVdpaNicInfoByHost(HostInventory host) {
        List<MevocoKVMAgentCommands.OvsDpdkBridgeTO> bridgeInfo = new ArrayList<>();

        Map<String, MevocoKVMAgentCommands.OvsDpdkBridgeTO> bridgeInfoMap = new HashMap();

        if (!host.getHypervisorType().equals(KVMConstant.KVM_HYPERVISOR_TYPE)) {
            return bridgeInfo;
        }

        //if l2 attach to cluster, will exsit bridge
        List<L2NetworkVO> l2s = SQL.New("select distinct l2 from L2NetworkVO l2, L2NetworkClusterRefVO ref where" +
                        " l2.uuid = ref.l2NetworkUuid" +
                        " and l2.vSwitchType = :vSwitchType" +
                        " and ref.clusterUuid = :clusterUuid")
                .param("vSwitchType", L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK)
                .param("clusterUuid", host.getClusterUuid())
                .list();
        if (!l2s.isEmpty()) {
            for (L2NetworkVO l2 : l2s) {
                MevocoKVMAgentCommands.OvsDpdkBridgeTO ovsDpdkBridgeTO = bridgeInfoMap.get(l2.getPhysicalInterface());
                if (ovsDpdkBridgeTO == null) {
                    MevocoKVMAgentCommands.OvsDpdkBridgeTO bridgeTO = new MevocoKVMAgentCommands.OvsDpdkBridgeTO();
                    bridgeInfoMap.put(l2.getPhysicalInterface(), bridgeTO);
                    bridgeInfo.add(bridgeTO);
                    ovsDpdkBridgeTO = bridgeTO;
                }
                ovsDpdkBridgeTO.setName(makeOvsBridgeName(l2.getUuid()));
                ovsDpdkBridgeTO.setPhysicalInterface(l2.getPhysicalInterface());
                boolean isHostNetworkInterface = Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.interfaceName, l2.getPhysicalInterface()).eq(HostNetworkInterfaceVO_.hostUuid, host.getUuid())
                        .isExists();
                ovsDpdkBridgeTO.setPhyType(isHostNetworkInterface ? "normal" : "bond");
                ovsDpdkBridgeTO.setPorts(new ArrayList<>());
            }
        }

        List<VmInstanceVO> vms = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.hostUuid, host.getUuid())
                .in(VmInstanceVO_.state, Arrays.asList(VmInstanceState.Running, VmInstanceState.Unknown)).list();

        for (VmInstanceVO vm : vms) {
            for (VmNicVO nic : vm.getVmNics()) {
                if (!nic.getType().equals(VmOvsNicConstant.ACCEL_TYPE_VDPA)) {
                    continue;
                }

                String l2NetworkUuid = Q.New(L3NetworkVO.class).select(L3NetworkVO_.l2NetworkUuid)
                        .eq(L3NetworkVO_.uuid, nic.getL3NetworkUuid()).findValue();

                L2NetworkVO l2 = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l2NetworkUuid).find();
                //check if physical interface is pre vdpa setting
                if (l2 == null) {
                    continue;
                }

                String phyNicName = l2.getPhysicalInterface();

                if (phyNicName.isEmpty()) {
                    continue;
                }

                MevocoKVMAgentCommands.OvsDpdkPortTO portTO = getOvsDpdkPortByNicVO(l2, nic);
                if (portTO == null) {
                    continue;
                }

                MevocoKVMAgentCommands.OvsDpdkBridgeTO ovsDpdkBridgeTO = bridgeInfoMap.get(phyNicName);
                if (ovsDpdkBridgeTO == null) {
                    logger.error(String.format("l2[uuid:%s, physical interface:%s] not attached to cluster", l2.getUuid(), l2.getPhysicalInterface()));
                    continue;
                }
                ovsDpdkBridgeTO.getPorts().add(portTO);

            }
        }

        return bridgeInfo;
    }

    private MevocoKVMAgentCommands.OvsDpdkPortTO getOvsDpdkPortByNicVO(L2NetworkVO l2, VmNicVO vmNicVO) {
        MevocoKVMAgentCommands.OvsDpdkPortTO ovsDpdkPortTO = new MevocoKVMAgentCommands.OvsDpdkPortTO();

        String pciAddress = SQL.New("select pci.pciDeviceAddress from PciDeviceVO pci, VmVdpaNicVO vdpa where vdpa.pciDeviceUuid = pci.uuid and" +
                        " vdpa.uuid = :vdpaUuid", String.class)
                .param("vdpaUuid", vmNicVO.getUuid())
                .find();
        if (pciAddress == null) {
            return null;
        }
        ovsDpdkPortTO.setType(vmNicVO.getType());
        ovsDpdkPortTO.setNicInternalName(vmNicVO.getInternalName());
        ovsDpdkPortTO.setVmUuid(vmNicVO.getVmInstanceUuid());
        Integer vlanId = Q.New(L2VlanNetworkVO.class).eq(L2VlanNetworkVO_.uuid, l2.getUuid()).select(L2VlanNetworkVO_.vlan).findValue();
        if (vlanId != null) {
            ovsDpdkPortTO.setVlanId(vlanId);
        }
        ovsDpdkPortTO.setPciDeviceAddress(pciAddress);
        ovsDpdkPortTO.setBridgeName(makeOvsBridgeName(l2.getUuid()));
        ovsDpdkPortTO.setPhysicalInterface(l2.getPhysicalInterface());

        MevocoKVMAgentCommands.OvsDpdkPortTO.VHostAddOn vHostAddOn = new MevocoKVMAgentCommands.OvsDpdkPortTO.VHostAddOn();
        vHostAddOn.setQueueNum(rcf.getResourceConfigValue(VmGlobalConfig.VM_NIC_MULTIQUEUE_NUM, vmNicVO.getVmInstanceUuid(), Integer.class));
        ovsDpdkPortTO.setvHostAddOn(vHostAddOn);
        return  ovsDpdkPortTO;
    }

    private static String makeOvsBridgeName(String l2Uuid) {
        return KVMHostUtils.getNormalizedBridgeName(l2Uuid, "br_%s");
    }

    private void syncVdpaToHost(Map<String, List<MevocoKVMAgentCommands.OvsDpdkBridgeTO>> hostVdpaInfoMaps, Completion completion) {
        if (hostVdpaInfoMaps == null || hostVdpaInfoMaps.isEmpty()) {
            completion.success();
            return;
        }

        List<KVMHostAsyncHttpCallMsg> msgs = new ArrayList<>();
        for (String hostUuid : hostVdpaInfoMaps.keySet()) {
            MevocoKVMAgentCommands.SyncVdpaCmd syncVdpaCmd = new MevocoKVMAgentCommands.SyncVdpaCmd();
            syncVdpaCmd.setOvsBridgeInfo(hostVdpaInfoMaps.get(hostUuid));

            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setNoStatusCheck(true);
            msg.setHostUuid(hostUuid);
            msg.setCommand(syncVdpaCmd);

            msg.setPath(MevocoKVMConstant.KVM_SYNC_VDPA_PATH);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
            msgs.add(msg);
        }

        new While<>(msgs).step((msg, cmpl) ->{
            bus.send(msg, new CloudBusCallBack(cmpl) {
                @Override
                public void run(MessageReply reply) {
                    MevocoKVMAgentCommands.SyncVdpaCmd cmd = JSONObjectUtil.toObject(
                            msg.getCommand(), MevocoKVMAgentCommands.SyncVdpaCmd.class);
                    if (!reply.isSuccess()) {
                        logger.error(String.format("failed to sync vdpa in host[uuid:%s, vdpa info:%s]: %s",
                                msg.getHostUuid(), cmd.getOvsBridgeInfo(), reply.getError()));
                    } else {
                        KVMHostAsyncHttpCallReply rly = reply.castReply();
                        MevocoKVMAgentCommands.SyncVdpaRsp rsp = rly.toResponse(
                                MevocoKVMAgentCommands.SyncVdpaRsp.class);
                        if (!rsp.isSuccess()) {
                            logger.error(String.format("failed to sync vdpa in host[uuid:%s, vdpa info:%s]: %s",
                                    msg.getHostUuid(), cmd.getOvsBridgeInfo(), rsp.getError()));
                        } else {
                            logger.debug(String.format("successfully sync vdpa in host[uuid:%s, nic:%s]",
                                    msg.getHostUuid(), cmd.getOvsBridgeInfo()));
                        }
                    }

                    cmpl.done();
                }
            });
        }, msgs.size()).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    @Override
    public List<String> appendExtraPackages(HostInventory host) {
        boolean isOvsDpdkSup = rcf.getResourceConfigValue(MevocoClusterGlobalConfig.OVS_DPDK_SUPPORT, host.getClusterUuid(), Boolean.class);
        if (isOvsDpdkSup) {
            return VmVdpaNicConstant.SMART_NIC_DEPENDENCIES;
        }
        return null;
    }

    @Override
    public void modifyDeploymentArguments(HostInventory host, KVMHostDeployArguments args) {

    }
}

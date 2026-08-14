package org.zstack.compute.sriov;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.host.ReserveEthernetVfMsg;
import org.zstack.compute.host.ReserveHostPciDeviceReply;
import org.zstack.compute.vm.VmNicManager;
import org.zstack.compute.vm.VmNicPrepareResourceExtensionPoint;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery.Od;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.AfterSyncPciDeviceExtensionPoint;
import org.zstack.header.host.HostAfterConnectedExtensionPoint;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.sriov.*;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagLifeCycleListener;
import org.zstack.header.vdpa.VmVdpaNicVO;
import org.zstack.header.vdpa.VmVdpaNicVO_;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.*;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.pciDevice.*;
import org.zstack.tag.TagManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.Tuple;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;

/**
 * Created by GuoYi on 12/9/19.
 */
public class VmVfNicManagerImpl extends AbstractService implements VmVfNicManager,
        ReleaseNetworkServiceOnDetachingNicExtensionPoint,
        PreVmInstantiateResourceExtensionPoint,
        VmReleaseResourceExtensionPoint,
        KvmPreAttachNicExtensionPoint,
        KvmPreDetachNicExtensionPoint,
        KVMStartVmExtensionPoint,
        FilterVmNicChangeableL3NetworkExtensionPoint,
        VmNicPrepareResourceExtensionPoint,
        VmFailToAttachL3NetworkExtensionPoint,
        VmInstanceMigrateExtensionPoint,
        AfterSyncPciDeviceExtensionPoint,
        PostVmInstantiateResourceExtensionPoint,
        VmDetachNicExtensionPoint,
        HostAfterConnectedExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmVfNicManagerImpl.class);
    private static final VfPciDeviceUtils vfPciDeviceUtils = new VfPciDeviceUtils();
    private Map<String, VmVfNicHypervisorBackend> vmVfNicHypervisorBackends = new HashMap<>();

    @Autowired
    private CloudBus bus;
    @Autowired
    private EventFacade evf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private VmNicManager nicManager;
    @Autowired
    private VmVfNicKvmBackend vfNicKvmBackend;
    @Autowired
    @Qualifier("KVMHostFactory")
    protected KVMHostFactory factory;

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeVmNicTypeMsg) {
            handle((APIChangeVmNicTypeMsg) msg);
        } else if (msg instanceof APIIsVfNicAvailableInL3NetworkMsg) {
            handle((APIIsVfNicAvailableInL3NetworkMsg) msg);
        } else if (msg instanceof APIChangeVfNicHaStateMsg) {
            handle((APIChangeVfNicHaStateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof ChangeVfNicHaStateMsg) {
            handle((ChangeVfNicHaStateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Transactional
    private void deleteVmVfNic(final String uuid) {
        String sql = "delete from VmVfNicVO where uuid = :uuid";
        Query q = dbf.getEntityManager().createNativeQuery(sql);
        q.setParameter("uuid", uuid);
        q.executeUpdate();
    }

    private void doChangeVfNicHaState(ChangeVfNicHaStateMsg msg, Completion completion) {
        VmVfNicInventory nic = VmVfNicInventory.valueOf(dbf.findByUuid(msg.getVfNicUuid(), VmVfNicVO.class));

        String vfNicOldState = nic.getHaState();
        if (vfNicOldState.equals(msg.getHaState()) && !msg.getForceUpdate()) {
            logger.debug(String.format("vf nic[uuid:%s] ha state is already %s", msg.getVfNicUuid(), msg.getHaState()));
            completion.success();
            return;
        }

        VmInstanceState vmState = Q.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, nic.getVmInstanceUuid())
                        .select(VmInstanceVO_.state)
                        .findValue();
        if (VmInstanceState.Stopped.equals(vmState)) {
            SQL.New(VmVfNicVO.class).eq(VmVfNicVO_.uuid, msg.getVfNicUuid())
                        .set(VmVfNicVO_.haState, VmVfNicHaState.valueOf(msg.getHaState()))
                        .update();
            logger.debug(String.format("vf nic[uuid:%s] ha state is changed to %s successfully",
                        msg.getVfNicUuid(),msg.getHaState()));
            completion.success();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("change-vf-nic-%s-ha-state-%s", msg.getVfNicUuid(), msg.getHaState()));
        chain.then(new NoRollbackFlow() {
            String __name__ = String.format("check-vf-nic-state-on-host");

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmInstanceSpec spec = new VmInstanceSpec();
                spec.setVmInventory(dbf.findByUuid(nic.getVmInstanceUuid(), VmInstanceVO.class).toInventory());
                spec.setDestNics(Arrays.asList(nic));
                data.put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);

                data.put(VmVfNicConstant.Params.VmVfNicInventory.toString(), nic);
                data.put(VmVfNicConstant.Params.VmVfNicHaState.toString(), msg.getHaState());

                trigger.next();
            }
        });

        chain.then(new VmVfNicChangeHaStateFlow());
        chain.then(new VmVfNicHaStateCallExtensionFlow());

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                SQL.New(VmVfNicVO.class).eq(VmVfNicVO_.uuid, msg.getVfNicUuid())
                        .set(VmVfNicVO_.haState, VmVfNicHaState.valueOf(msg.getHaState()))
                        .update();
                logger.debug(String.format("vf nic[uuid:%s] ha state is changed to %s successfully", msg.getVfNicUuid(),
                        msg.getHaState()));
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(ChangeVfNicHaStateMsg msg) {
        ChangeVfNicHaStateReply reply = new ChangeVfNicHaStateReply();

        String vmUuid = Q.New(VmVfNicVO.class).eq(VmVfNicVO_.uuid, msg.getVfNicUuid()).select(VmVfNicVO_.vmInstanceUuid)
                .findValue();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("changeVfNicHaState-%s", vmUuid);
            }

            @Override
            public void run(final SyncTaskChain chain) {
                doChangeVfNicHaState(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        reply.setInventory(
                                VmVfNicInventory.valueOf(dbf.findByUuid(msg.getVfNicUuid(), VmVfNicVO.class)));
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

    private void handle(APIChangeVfNicHaStateMsg msg) {
        final APIChangeVfNicHaStateEvent evt = new APIChangeVfNicHaStateEvent(msg.getId());

        ChangeVfNicHaStateMsg cmsg = new ChangeVfNicHaStateMsg();
        cmsg.setVfNicUuid(msg.getVfNicUuid());
        cmsg.setHaState(msg.getHaState().toString());
        bus.makeTargetServiceIdByResourceUuid(cmsg, VmVfNicConstant.SERVICE_ID, msg.getVfNicUuid());
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    evt.setInventory(((ChangeVfNicHaStateReply) reply).getInventory());
                } else {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(APIChangeVmNicTypeMsg msg) {
        APIChangeVmNicTypeEvent event = new APIChangeVmNicTypeEvent(msg.getId());
        VmNicVO nic = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
        if (nic == null) {
            event.setError(operr("vm nic[uuid:%s] doesn't exist", msg.getVmNicUuid()));
            bus.publish(event);
            return;
        }

        if (nic.getType().equals(msg.getVmNicType())) {
            logger.debug(String.format("vm nic[uuid:%s] is already of type %s, no need to change", msg.getVmNicUuid(), msg.getVmNicType()));
            event.setInventory(VmNicInventory.valueOf(nic));
            bus.publish(event);
            return;
        }

        nic.setType(msg.getVmNicType());
        nic.setDriverType(VmSystemTags.VIRTIO.hasTag(nic.getVmInstanceUuid()) ? nicManager.getDefaultPVNicDriver()
                : nicManager.getDefaultNicDriver());
        nic = dbf.updateAndRefresh(nic);
        event.setInventory(VmNicInventory.valueOf(nic));

        if (!msg.getVmNicType().equals(VmVfNicConstant.VIRTUAL_FUNCTION_TYPE)) {
            deleteVmVfNic(nic.getUuid());
        }
        bus.publish(event);
    }

    private void handle(APIIsVfNicAvailableInL3NetworkMsg msg) {
        APIIsVfNicAvailableInL3NetworkReply reply = new APIIsVfNicAvailableInL3NetworkReply();

        reply.setVfNicAvailable(vfPciDeviceUtils.hasAvailableVfDevice(msg.getHostUuid(), msg.getL3NetworkUuid()));
        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VmVfNicConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateExtensions();
        installSystemTagValidator();
        initSystemTags();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void populateExtensions() {
        for (VmVfNicHypervisorBackend bkd : pluginRgty.getExtensionList(VmVfNicHypervisorBackend.class)) {
            String type = bkd.getHypervisorType().toString();
            VmVfNicHypervisorBackend old = vmVfNicHypervisorBackends.get(type);
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VmVfNicHypervisorBackend[%s, %s] for type[%s]",
                        bkd.getClass().getName(), old.getClass().getName(), type));
            }
            vmVfNicHypervisorBackends.put(type, bkd);
        }
    }

    private void installSystemTagValidator() {
        SriovSystemTags.L2_ENABLE_SRIOV.installValidator((resourceUuid, resourceType, systemTag) -> {

            String l2Type = Q.New(L2NetworkVO.class)
                    .eq(L2NetworkVO_.uuid, resourceUuid)
                    .select(L2NetworkVO_.type)
                    .findValue();
            if (!L2NetworkType.getSriovSupportedTypeNames().contains(l2Type)) {
                throw new ApiMessageInterceptionException(argerr("only %s support sriov", L2NetworkType.getSriovSupportedTypeNames()));
            }

            String vSwitchType = Q.New(L2NetworkVO.class)
                    .eq(L2NetworkVO_.uuid, resourceUuid)
                    .select(L2NetworkVO_.vSwitchType)
                    .findValue();
            if (L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK.equals(vSwitchType)) {
                throw new ApiMessageInterceptionException(
                        argerr("%s don't support sr-iov", L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK));
            }
        });
    }

    private void initSystemTags() {
        SystemTagLifeCycleListener listener = new SystemTagLifeCycleListener() {
            @Override
            public void tagCreated(SystemTagInventory tag) {
                thdf.chainSubmit(new ChainTask(null) {
                    @Override
                    public String getSyncSignature() {
                        return "add-bridge-fdb-entry-for-l2-" + tag.getResourceUuid();
                    }

                    @Override
                    public void run(SyncTaskChain chain) {
                        FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
                        fchain.setName("add-bridge-fdb-entry-for-l2-" + tag.getResourceUuid());
                        fchain.then(new NoRollbackFlow() {
                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                addBridgeFdbEntryForExistingVmNicsInL2Network(tag.getResourceUuid(), new Completion(trigger) {
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
                                addBridgeFdbEntryForExistingInnerNicInL2Network(tag.getResourceUuid(), new Completion(trigger) {
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
                        }).done(new FlowDoneHandler(chain) {
                            @Override
                            public void handle(Map data) {
                                chain.next();
                            }
                        }).error(new FlowErrorHandler(chain) {
                            @Override
                            public void handle(ErrorCode errCode, Map data) {
                                chain.next();
                            }
                        }).start();
                    }

                    @Override
                    public String getName() {
                        return getSyncSignature();
                    }
                });
            }

            @Override
            public void tagDeleted(SystemTagInventory tag) {

            }

            @Override
            public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {

            }
        };

        //SriovSystemTags.L2_ENABLE_SRIOV.installLifeCycleListener(listener);
    }

    private void addBridgeFdbEntryForExistingVmNicsInL2Network(String l2Uuid, Completion completion) {
        List<String> l3Uuids = Q.New(L3NetworkVO.class)
                .eq(L3NetworkVO_.l2NetworkUuid, l2Uuid)
                .select(L3NetworkVO_.uuid)
                .listValues();
        if (l3Uuids.isEmpty()) {
            completion.success();
            return;
        }

        List<Tuple> vmNicMacs = Q.New(VmNicVO.class)
                .in(VmNicVO_.l3NetworkUuid, l3Uuids)
                .eq(VmNicVO_.type, VmInstanceConstant.VIRTUAL_NIC_TYPE)
                .select(VmNicVO_.mac, VmNicVO_.vmInstanceUuid)
                .listTuple();
        if (vmNicMacs.isEmpty()) {
            completion.success();
            return;
        }

        List<String> vmUuids = vmNicMacs.stream().map(m -> m.get(1, String.class)).distinct()
                .collect(Collectors.toList());
        if (vmUuids.isEmpty()) {
            completion.success();
            return;
        }

        String physicalInterface = Q.New(L2NetworkVO.class)
                .eq(L2NetworkVO_.uuid, l2Uuid)
                .select(L2NetworkVO_.physicalInterface)
                .findValue();

        List<Tuple> vms = Q.New(VmInstanceVO.class)
                .in(VmInstanceVO_.uuid, vmUuids)
                .select(VmInstanceVO_.uuid, VmInstanceVO_.hypervisorType, VmInstanceVO_.hostUuid)
                .listTuple();

        List<String> hypervisorTypes = vms.stream().map(vm -> vm.get(1, String.class)).distinct()
                .collect(Collectors.toList());
        for (String hypervisorType : hypervisorTypes) {
            Map<String, List<String>> hostNicMacs = new HashMap<>();
            for (Tuple vm : vms.stream().filter(vm -> vm.get(1, String.class).equals(hypervisorType))
                    .collect(Collectors.toList())) {
                String vmUuid = vm.get(0, String.class);
                String hostUuid = vm.get(2, String.class);
                if (!hostNicMacs.containsKey(hostUuid)) {
                    hostNicMacs.put(hostUuid, new ArrayList<>());
                }

                hostNicMacs.get(hostUuid).addAll(vmNicMacs.stream()
                        .filter(m -> m.get(1, String.class).equals(vmUuid))
                        .map(m -> m.get(0, String.class)).collect(Collectors.toList()));
            }

            VmVfNicHypervisorBackend bkd = vmVfNicHypervisorBackends.get(hypervisorType);
            if (bkd == null) {
                logger.debug(String.format("cannod find vf nic hypervisor backend of type %s", hypervisorType));
            } else {
                bkd.addBridgeFdbEntryForVmNics(hostNicMacs, physicalInterface, completion);
            }
        }
    }

    private void addBridgeFdbEntryForExistingInnerNicInL2Network(String l2Uuid, Completion completion) {
        /* inner nic include: eip namespace nics and dhcp namespace nics
        * these are handled when they are created or host connected, */
        completion.success();
    }

    @Override
    public void preAttachNicExtensionPoint(KVMHostInventory host, KVMAgentCommands.AttachNicCommand cmd) {
        /*
         * Update vf nic pci device address etc. in AttachNicCommand.
         */
        String nicUuid = cmd.getNic().getUuid();

        VmVfNicVO vf = dbf.findByUuid(nicUuid, VmVfNicVO.class);
        if (vf == null) {
            return;
        }

        String pciAddress = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.uuid, vf.getPciDeviceUuid())
                .select(PciDeviceVO_.pciDeviceAddress)
                .findValue();
        cmd.getNic().setPciDeviceAddress(pciAddress);

        String l2Uuid = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, vf.getL3NetworkUuid())
                .select(L3NetworkVO_.l2NetworkUuid).findValue();
        L2VlanNetworkVO l2 = dbf.findByUuid(l2Uuid, L2VlanNetworkVO.class);
        if (l2 != null) {
            cmd.getNic().setVlanId(String.valueOf(l2.getVlan()));
        }

        logger.debug(
                String.format("updated vf nic pci device address in attachNicCmd for vm[uuid:%s]", cmd.getVmUuid()));
    }

    @Override
    public void preDetachNicExtensionPoint(KVMHostInventory host, KVMAgentCommands.DetachNicCommand cmd) {
        /*
         * Update vf nic pci device address etc. in DetachNicCommand.
         */
        String nicUuid = cmd.getNic().getUuid();

        VmVfNicVO vf = dbf.findByUuid(nicUuid, VmVfNicVO.class);
        if (vf == null) {
            return;
        }

        String pciAddress = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.uuid, vf.getPciDeviceUuid())
                .select(PciDeviceVO_.pciDeviceAddress)
                .findValue();
        cmd.getNic().setPciDeviceAddress(pciAddress);

        String l2Uuid = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, vf.getL3NetworkUuid())
                .select(L3NetworkVO_.l2NetworkUuid).findValue();
        L2VlanNetworkVO l2 = dbf.findByUuid(l2Uuid, L2VlanNetworkVO.class);
        if (l2 != null) {
            cmd.getNic().setVlanId(String.valueOf(l2.getVlan()));
        }

        logger.debug(
                String.format("updated vf nic pci device address in detachNicCmd for vm[uuid:%s]", cmd.getVmUuid()));
    }

    @Override
    public void releaseResourceOnDetachingNic(VmInstanceSpec spec, VmNicInventory nic, NoErrorCompletion completion) {
        if (!nic.getType().equals(VmVfNicConstant.VIRTUAL_FUNCTION_TYPE)) {
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
        List<VmVfNicVO> vfNics = Q.New(VmVfNicVO.class).eq(VmVfNicVO_.vmInstanceUuid, vmUuid).list();
        if (vfNics.isEmpty()) {
            completion.success();
            return;
        }

        ReserveEthernetVfMsg msg = new ReserveEthernetVfMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmUuid(vmUuid);
        msg.setL3Uuids(vfNics.stream().map(VmVfNicVO::getL3NetworkUuid).collect(Collectors.toList()));
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

                    for (VmVfNicVO vfNic : vfNics) {
                        if (vfPciDeviceVO.getL3NetworkUuid().equals(vfNic.getL3NetworkUuid())) {
                            logger.debug(String.format("set vm[name:%s] nic [name:%s, l3Uuid:%s] vf pci address[:%s]]",
                                    spec.getVmInventory().getName(), vfNic.getInternalName(), vfNic.getL3NetworkUuid(),
                                    vfPciDeviceVO.getPciDeviceAddress()));
                            vfNic.setPciDeviceUuid(vfPciDeviceVO.getUuid());
                        }
                    }
                }

                dbf.updateCollection(vfNics);

                completion.success();
            }
        });
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }

    @Override
    public void postBeforeInstantiateVmResource(VmInstanceSpec spec) {

    }

    @Override
    public void postInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        String vmUuid = spec.getVmInventory().getUuid();
        List<VmVfNicVO> vfNics = Q.New(VmVfNicVO.class)
                        .eq(VmVfNicVO_.vmInstanceUuid, vmUuid)
                        .orderBy(VmVfNicVO_.deviceId, Od.ASC)
                        .list();
        if (vfNics.isEmpty()) {
            completion.success();
            return;
        }

        List<ChangeVfNicHaStateMsg> cmsgs = new ArrayList<>();
        vfNics.stream().filter(vfNic -> vfNic.getHaState().equals(VmVfNicHaState.Enabled)).forEach(vfNic -> {
            ChangeVfNicHaStateMsg cmsg = new ChangeVfNicHaStateMsg();
            cmsg.setVfNicUuid(vfNic.getUuid());
            cmsg.setHaState(VmVfNicHaState.Enabled.toString());
            cmsg.setForceUpdate(true);
            cmsgs.add(cmsg);
        });

        new While<>(cmsgs).each((cmsg, wcomp) -> {
            bus.makeTargetServiceIdByResourceUuid(cmsg, VmVfNicConstant.SERVICE_ID, vmUuid);
            bus.send(cmsg, new CloudBusCallBack(wcomp) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        wcomp.addError(reply.getError());
                        wcomp.done();
                        return;
                    }
                    wcomp.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList != null && !errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });
    }

    @Override
    public void postReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        /*
         * Update vf nic pci device address etc. in StartVmCmd.
         */
        String vmUuid = spec.getVmInventory().getUuid();
        List<String> vfNicUuids = Q.New(VmVfNicVO.class).eq(VmVfNicVO_.vmInstanceUuid, vmUuid).select(VmVfNicVO_.uuid)
                .listValues();

        if (vfNicUuids.isEmpty()) {
            return;
        }

        for (String vfNicUuid : vfNicUuids) {
            Optional<KVMAgentCommands.NicTO> nic = cmd.getNics().stream().filter(n -> n.getUuid().equals(vfNicUuid))
                    .findFirst();
            if (!nic.isPresent()) {
                logger.error(
                        String.format("vm[uuid:%s] has vnic[uuid:%s] but it doesn't appears in the start vm cmd %s",
                                vmUuid, vfNicUuid, cmd));
            } else {
                KVMAgentCommands.NicTO nicTO = nic.get();

                VmVfNicVO vfVO = dbf.findByUuid(vfNicUuid, VmVfNicVO.class);
                DebugUtils.Assert(vfVO != null, String.format("vf nic[uuid:%s] doesn't exists", vfNicUuid));

                String pciAddress = Q.New(PciDeviceVO.class)
                        .eq(PciDeviceVO_.uuid, vfVO.getPciDeviceUuid())
                        .select(PciDeviceVO_.pciDeviceAddress)
                        .findValue();
                nicTO.setPciDeviceAddress(pciAddress);

                String l2Uuid = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, vfVO.getL3NetworkUuid())
                        .select(L3NetworkVO_.l2NetworkUuid).findValue();
                L2VlanNetworkVO l2 = dbf.findByUuid(l2Uuid, L2VlanNetworkVO.class);
                if (l2 != null) {
                    nicTO.setVlanId(String.valueOf(l2.getVlan()));
                }
            }
        }

        logger.debug(String.format("updated vf nic pci device address in startVmCmd for vm[uuid:%s]", vmUuid));
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {
    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {
    }

    @Override
    public void releaseVmResource(VmInstanceSpec spec, Completion completion) {
        // if vm stopped, do not release vf nic pci devices
        if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.Destroy) {
            vfPciDeviceUtils.releaseVfDevice(spec.getVmInventory());
        }

        completion.success();
    }

    @Override
    public List<L3NetworkInventory> filterVmNicChangeableL3Network(VmNicInventory nic, List<L3NetworkInventory> l3s) {
        boolean isSriov = Q.New(VmVfNicVO.class).eq(VmVfNicVO_.uuid, nic.getUuid()).isExists();

        if (!isSriov) {
            return l3s;
        }
        Iterator<L3NetworkInventory> it = l3s.iterator();
        while (it.hasNext()) {
            if (!SriovSystemTags.L2_ENABLE_SRIOV.hasTag(it.next().getL2NetworkUuid())) {
                it.remove();
            }
        }
        return l3s;
    }


    @Override
    @Transactional
    public void afterHostConnected(HostInventory host) {
        if (!host.getHypervisorType().equals(KVMConstant.KVM_HYPERVISOR_TYPE)) {
            return;
        }

        /* create EthernetVfPciDeviceVO */
        List<String> vfUuids = Q.New(EthernetVfPciDeviceVO.class)
                .eq(EthernetVfPciDeviceVO_.hostUuid, host.getUuid())
                .select(EthernetVfPciDeviceVO_.uuid)
                .listValues();

        List<PciDeviceVO> vfPciDeviceVos;
        if (vfUuids.isEmpty()) {
            vfPciDeviceVos = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.hostUuid, host.getUuid())
                    .eq(PciDeviceVO_.type, PciDeviceType.Ethernet_Controller)
                    .notNull(PciDeviceVO_.parentUuid)
                    .list();
        } else {
            vfPciDeviceVos = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.hostUuid, host.getUuid())
                    .eq(PciDeviceVO_.type, PciDeviceType.Ethernet_Controller)
                    .notIn(PciDeviceVO_.uuid, vfUuids)
                    .notNull(PciDeviceVO_.parentUuid)
                    .list();
        }

        if (vfPciDeviceVos.isEmpty()) {
            return;
        }

        Map<String, String> pciAddressUuidsMap = new HashMap<>();
        List<Tuple> pfPciDeviceVos = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.hostUuid, host.getUuid())
                .eq(PciDeviceVO_.type, PciDeviceType.Ethernet_Controller)
                .isNull(PciDeviceVO_.parentUuid)
                .select(PciDeviceVO_.uuid, PciDeviceVO_.pciDeviceAddress)
                .listTuple();
        for (Tuple t : pfPciDeviceVos) {
            pciAddressUuidsMap.put(t.get(1, String.class), t.get(0, String.class));
        }

        Map<String, String> uuidPfNameMap = new HashMap<>();
        List<Tuple> pfInterfaceVos = Q.New(HostNetworkInterfaceVO.class)
                .eq(HostNetworkInterfaceVO_.hostUuid, host.getUuid())
                .in(HostNetworkInterfaceVO_.pciDeviceAddress, pciAddressUuidsMap.keySet())
                .select(HostNetworkInterfaceVO_.pciDeviceAddress, HostNetworkInterfaceVO_.interfaceName)
                .listTuple();
        for (Tuple t : pfInterfaceVos) {
            String pciAddress = t.get(0, String.class);
            String pfName = t.get(1, String.class);
            String uuid = pciAddressUuidsMap.get(pciAddress);
            uuidPfNameMap.put(uuid, pfName);
        }

        for (PciDeviceVO vfPci : vfPciDeviceVos) {
            String l3Uuid = null;
            String vmUuid = null;
            Tuple t = Q.New(VmVfNicVO.class)
                    .eq(VmVfNicVO_.pciDeviceUuid, vfPci.getUuid())
                    .select(VmVfNicVO_.vmInstanceUuid, VmVfNicVO_.l3NetworkUuid)
                    .findTuple();
            if (t != null) {
                vmUuid = t.get(0, String.class);
                l3Uuid = t.get(1, String.class);
            } else {
                t = Q.New(VmVdpaNicVO.class)
                        .eq(VmVdpaNicVO_.pciDeviceUuid, vfPci.getUuid())
                        .select(VmVdpaNicVO_.vmInstanceUuid, VmVdpaNicVO_.l3NetworkUuid)
                        .findTuple();
                if (t != null) {
                    vmUuid = t.get(0, String.class);
                    l3Uuid = t.get(1, String.class);
                }
            }

            String sql;
            if (vmUuid == null) {
                sql = String.format("insert into EthernetVfPciDeviceVO " +
                                "(uuid, hostDevUuid, interfaceName, vmUuid, l3NetworkUuid," +
                                " vfStatus) values ('%s', '%s', '%s', null, null, 'Available')",
                        vfPci.getUuid(), host.getUuid(),
                        uuidPfNameMap.get(vfPci.getParentUuid()),
                        vmUuid, l3Uuid);
            } else {
                sql = String.format("insert into EthernetVfPciDeviceVO " +
                                "(uuid, hostDevUuid, interfaceName, vmUuid, l3NetworkUuid," +
                                " vfStatus) values ('%s', '%s', '%s', '%s', '%s', 'Attached')",
                        vfPci.getUuid(), host.getUuid(),
                        uuidPfNameMap.get(vfPci.getParentUuid()),
                        vmUuid, l3Uuid);
            }
            dbf.getEntityManager().createNativeQuery(sql).executeUpdate();
        }
    }

    @Override
    public void afterSyncPciDeviceVO(String hostUuid, List<PciDeviceTO> tos) {
        List<EthernetVfPciDeviceVO> vfPciDeviceVOS = Q.New(EthernetVfPciDeviceVO.class)
                .eq(EthernetVfPciDeviceVO_.hostUuid, hostUuid).list();
        if (vfPciDeviceVOS.isEmpty()) {
            return;
        }

        List<String> pciUuids = vfPciDeviceVOS.stream().map(EthernetVfPciDeviceVO::getUuid)
                .collect(Collectors.toList());
        Map<String, String> pciDeviceVmUuidMap = new HashMap<>();
        Map<String, String> pciDeviceL3UuidMap = new HashMap<>();
        List<Tuple> vfNics = Q.New(VmVfNicVO.class)
                .select(VmVfNicVO_.pciDeviceUuid, VmVfNicVO_.vmInstanceUuid, VmVfNicVO_.l3NetworkUuid)
                .in(VmVfNicVO_.pciDeviceUuid, pciUuids).listTuple();
        for (Tuple t : vfNics) {
            String pciUuid = (String) t.get(0);
            String vmUuid = (String) t.get(1);
            String l3Uuid = (String) t.get(2);
            pciDeviceVmUuidMap.put(pciUuid, vmUuid);
            pciDeviceL3UuidMap.put(pciUuid, l3Uuid);
        }

        List<Tuple> vdpaNics = Q.New(VmVdpaNicVO.class)
                .select(VmVdpaNicVO_.pciDeviceUuid, VmVdpaNicVO_.vmInstanceUuid, VmVdpaNicVO_.l3NetworkUuid)
                .in(VmVdpaNicVO_.pciDeviceUuid, pciUuids).listTuple();
        for (Tuple t : vdpaNics) {
            String pciUuid = (String) t.get(0);
            String vmuuid = (String) t.get(1);
            String l3Uuid = (String) t.get(2);
            pciDeviceVmUuidMap.put(pciUuid, vmuuid);
            pciDeviceL3UuidMap.put(pciUuid, l3Uuid);
        }

        List<EthernetVfPciDeviceVO> updated = new ArrayList<>();
        for (EthernetVfPciDeviceVO pvo : vfPciDeviceVOS) {
            boolean update = false;

            String vmUuid = pciDeviceVmUuidMap.get(pvo.getUuid());
            if (vmUuid != null && !vmUuid.equals(pvo.getVmUuid())) {
                pvo.setVmInstanceUuid(vmUuid);
                pvo.setVmUuid(vmUuid);
                update = true;
            }
            String l3Uuid = pciDeviceL3UuidMap.get(pvo.getUuid());
            if (l3Uuid != null && !l3Uuid.equals(pvo.getL3NetworkUuid())) {
                pvo.setL3NetworkUuid(l3Uuid);
                update = true;
            }

            if (update) {
                updated.add(pvo);
            }
        }

        if (!updated.isEmpty()) {
            dbf.updateCollection(updated);
        }
    }

    @Override
    public Flow getPreparationFlow() {
        return new VmVfNicReserveFlow();
    }

    @Override
    public void vmFailToAttachL3Network(VmInstanceInventory vm, L3NetworkInventory l3, ErrorCode error) {
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        if (inv == null || destHostUuid == null) {
            return;
        }

        List<VmNicInventory> vfNics = inv.getVmNics().stream()
                .filter(nic -> nic.getType().equals(VmVfNicConstant.VIRTUAL_FUNCTION_TYPE))
                .collect(Collectors.toList());
        if (vfNics.isEmpty()) {
            return;
        }

        List<VmVfNicVO> vfVos = Q.New(VmVfNicVO.class)
                .in(VmVfNicVO_.uuid, vfNics.stream().map(VmNicInventory::getUuid).collect(Collectors.toList()))
                .orderBy(VmVfNicVO_.deviceId, Od.ASC)
                .list();

        FutureCompletion completion = new FutureCompletion(null);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("pre-handle-vf-nic-before-migrate-vm");
        chain.then(new Flow() {
            String __name__ = "change-vf-nic-ha-state-to-enable-if-need";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<ChangeVfNicHaStateMsg> cmsgs = new ArrayList<>();
                vfVos.stream().filter(vf -> !vf.getHaState().equals(VmVfNicHaState.Enabled)).forEach(vf -> {
                    ChangeVfNicHaStateMsg cmsg = new ChangeVfNicHaStateMsg();
                    cmsg.setVfNicUuid(vf.getUuid());
                    cmsg.setHaState(VmVfNicHaState.Enabled.toString());
                    bus.makeTargetServiceIdByResourceUuid(cmsg, VmVfNicConstant.SERVICE_ID, vf.getUuid());
                    cmsgs.add(cmsg);
                });

                if (cmsgs.isEmpty()) {
                    trigger.next();
                    return;
                }

                new While<>(cmsgs).each((cmsg, wcomp) -> {
                    bus.send(cmsg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                wcomp.addError(reply.getError());
                                wcomp.done();
                                return;
                            }

                            wcomp.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList != null && !errorCodeList.getCauses().isEmpty()) {
                            trigger.fail(errorCodeList.getCauses().get(0));
                        } else {
                            trigger.next();
                        }
                    }
                });

            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
            }
        });
        chain.then(new Flow() {
            String __name__ = "change-vf-nic-ha-state-to-disconnect-if-need";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<ChangeVfNicHaStateMsg> cmsgs = new ArrayList<>();
                vfVos.forEach(vf -> {
                    ChangeVfNicHaStateMsg cmsg = new ChangeVfNicHaStateMsg();
                    cmsg.setVfNicUuid(vf.getUuid());
                    cmsg.setHaState(VmVfNicHaState.Disconnecting.toString());
                    bus.makeTargetServiceIdByResourceUuid(cmsg, VmVfNicConstant.SERVICE_ID, vf.getUuid());
                    cmsgs.add(cmsg);
                });

                new While<>(cmsgs).each((cmsg, wcomp) -> {
                    bus.send(cmsg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                wcomp.addError(reply.getError());
                                wcomp.done();
                                return;
                            }

                            wcomp.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList != null && !errorCodeList.getCauses().isEmpty()) {
                            trigger.fail(errorCodeList.getCauses().get(0));
                        } else {
                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
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

        completion.await(TimeUnit.MINUTES.toMillis(5));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(
                    operr("failed to pre-handle vf nic before migrate vm[uuid:%s] to host[uuid:%s]",
                            inv.getUuid(), destHostUuid).causedBy(completion.getErrorCode()));
        }
    }

    @Override
    public void postMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        if (inv == null || destHostUuid == null) {
            return;
        }

        List<VmNicInventory> vfNics = inv.getVmNics().stream()
                .filter(nic -> nic.getType().equals(VmVfNicConstant.VIRTUAL_FUNCTION_TYPE))
                .collect(Collectors.toList());
        if (vfNics.isEmpty()) {
            return;
        }

        List<VmVfNicVO> vfVos = Q.New(VmVfNicVO.class)
                .in(VmVfNicVO_.uuid, vfNics.stream().map(VmNicInventory::getUuid).collect(Collectors.toList()))
                .orderBy(VmVfNicVO_.deviceId, Od.ASC)
                .list();

        FutureCompletion completion = new FutureCompletion(null);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();

        chain.setName("post-handle-vf-nic-after-migrate-vm");

        chain.then(new Flow() {
            String __name__ = "change-vf-nic-ha-state-to-reconnect-if-need";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<VmVfNicVO> toUpdate = new ArrayList<>();
                for (VmNicInventory nic : vfNics) {
                    PciDeviceInventory pciDevice = vfPciDeviceUtils.allocateReservedVfDevice(nic);
                    vfPciDeviceUtils.releaseToBeReleasedVfPciDevice(nic);
                    vfVos.stream().filter(vo -> vo.getUuid().equals(nic.getUuid())).findFirst().ifPresent(vo -> {
                        vo.setPciDeviceUuid(pciDevice.getUuid());
                        toUpdate.add(vo);
                    });
                }

                dbf.updateCollection(toUpdate);

                List<ChangeVfNicHaStateMsg> cmsgs = new ArrayList<>();
                vfVos.stream().forEach(vf -> {
                    ChangeVfNicHaStateMsg cmsg = new ChangeVfNicHaStateMsg();
                    cmsg.setVfNicUuid(vf.getUuid());
                    cmsg.setHaState(VmVfNicHaState.Reconnecting.toString());
                    bus.makeTargetServiceIdByResourceUuid(cmsg, VmVfNicConstant.SERVICE_ID, vf.getUuid());
                    cmsgs.add(cmsg);
                });

                if (cmsgs.isEmpty()) {
                    trigger.next();
                    return;
                }

                new While<>(cmsgs).each((cmsg, wcomp) -> {
                    bus.send(cmsg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                wcomp.addError(reply.getError());
                                wcomp.done();
                                return;
                            }

                            wcomp.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList != null && !errorCodeList.getCauses().isEmpty()) {
                            trigger.fail(errorCodeList.getCauses().get(0));
                        } else {
                            trigger.next();
                        }
                    }
                });

            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
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

        completion.await(TimeUnit.MINUTES.toMillis(5));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(
                    operr("failed to post-handle vf nic after migrate vm[uuid:%s] to host[uuid:%s]",
                            inv.getUuid(), destHostUuid).causedBy(completion.getErrorCode()));
        }
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        List<VmNicInventory> vfNics = inv.getVmNics().stream()
                .filter(nic -> nic.getType().equals(VmVfNicConstant.VIRTUAL_FUNCTION_TYPE))
                .collect(Collectors.toList());
        if (vfNics.isEmpty()) {
            return;
        }

        String currentHostUuid = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, inv.getUuid())
                .select(VmInstanceVO_.hostUuid).findValue();
        if (destHostUuid != null && !destHostUuid.equals(currentHostUuid)) {
            /* 释放新物理机的vf */
            vfPciDeviceUtils.releaseVfDevice(inv);
            /* 恢复旧的vf */
            for (VmNicInventory nic : vfNics) {
                vfPciDeviceUtils.restoreVfPciDevice(nic);
            }
        }
    }

    @Override
    public void preDetachNic(VmNicInventory nic) {
    }

    @Override
    public void beforeDetachNic(VmNicInventory nic) {
        if (!nic.getType().equals(VmVfNicConstant.VIRTUAL_FUNCTION_TYPE)) {
            return;
        }

        VmVfNicVO vfNic = dbf.findByUuid(nic.getUuid(), VmVfNicVO.class);
        if (vfNic.getHaState().equals(VmVfNicHaState.Disabled)) {
            return;
        }

        ChangeVfNicHaStateMsg msg = new ChangeVfNicHaStateMsg();
        msg.setVfNicUuid(nic.getUuid());
        msg.setHaState(VmVfNicHaState.Disabled.toString());
        bus.makeTargetServiceIdByResourceUuid(msg, VmVfNicConstant.SERVICE_ID, nic.getUuid());
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to change vf nic[uuid:%s] ha state to disconnecting, %s",
                            nic.getUuid(), reply.getError()));
                }
            }
        });
    }

    @Override
    public void afterDetachNic(VmNicInventory nic) {

    }

    @Override
    public void failedToDetachNic(VmNicInventory nic, ErrorCode error) {

    }
}

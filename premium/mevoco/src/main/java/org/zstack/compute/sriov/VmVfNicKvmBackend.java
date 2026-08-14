package org.zstack.compute.sriov;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.MevocoKVMAgentCommands;
import org.zstack.compute.host.MevocoKVMConstant;
import org.zstack.compute.host.PostHostConnectExtensionPoint;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.*;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.pciDevice.PciDeviceInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 4/13/20.
 */
public class VmVfNicKvmBackend implements VmVfNicHypervisorBackend, VmInstanceStartNewCreatedVmExtensionPoint,
        VmInstanceStartExtensionPoint, VmInstanceStopExtensionPoint, VmInstanceMigrateExtensionPoint,
        VmInstanceAttachNicExtensionPoint, VmDetachNicExtensionPoint, PostHostConnectExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmVfNicKvmBackend.class);
    private static final VfPciDeviceUtils vfPciDeviceUtils = new VfPciDeviceUtils();

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    enum VmVfNicFdbOperation {
        ADD,
        DELETE
    }

    @Override
    public void addBridgeFdbEntryForInnerNics(List<String> hostUuids, Completion completion) {
        if (hostUuids.isEmpty()) {
            completion.success();
            return;
        }

        logger.debug("addBridgeFdbEntryForInnerNics hostUuids = " + hostUuids);
        List<KVMHostAsyncHttpCallMsg> msgs = new ArrayList<>();
        for (String hostUuid : hostUuids) {
            // empty cmd means add bridge fdb entrys for all inner nics in the host
            MevocoKVMAgentCommands.AddBridgeFdbEntryCmd cmd = new MevocoKVMAgentCommands.AddBridgeFdbEntryCmd();
            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setHostUuid(hostUuid);
            msg.setCommand(cmd);
            msg.setPath(MevocoKVMConstant.ADD_BRIDGE_FDB_ENTRY_PATH);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
            msgs.add(msg);
        }

        new While<>(msgs).each((msg, cmpl) -> {
            bus.send(msg, new CloudBusCallBack(cmpl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.error(String.format("failed to add bridge fdb in host[uuid:%s] for inner nics: %s",
                                msg.getHostUuid(), reply.getError()));
                    } else {
                        KVMHostAsyncHttpCallReply rly = reply.castReply();
                        MevocoKVMAgentCommands.AddBridgeFdbEntryRsp rsp = rly.toResponse(
                                MevocoKVMAgentCommands.AddBridgeFdbEntryRsp.class);
                        if (!rsp.isSuccess()) {
                            logger.error(String.format("failed to add bridge fdb in host[uuid:%s] for inner nics: %s",
                                    msg.getHostUuid(), rsp.getError()));
                        } else {
                            logger.debug(String.format("successfully added bridge fdb in host[uuid:%s] for inner nics",
                                    msg.getHostUuid()));
                        }
                    }

                    cmpl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    @Override
    public void addBridgeFdbEntryForVmNics(Map<String, List<String>> hostNicMacs, String physicalInterface, Completion completion) {
        changeBridgeFdbEntryForVmNics(hostNicMacs, physicalInterface, VmVfNicFdbOperation.ADD, completion);
    }

    public void changeBridgeFdbEntryForVmNics(Map<String, List<String>> hostNicMacs, String physicalInterface, VmVfNicFdbOperation oper, Completion completion) {
        changeBridgeFdbEntryForVmNics(hostNicMacs, physicalInterface, oper, true, completion);
    }

    public void changeBridgeFdbEntryForVmNics(Map<String, List<String>> hostNicMacs, String physicalInterface,
                                              VmVfNicFdbOperation oper, boolean hostStatusCheck, Completion completion) {
        if (hostNicMacs.isEmpty()) {
            completion.success();
            return;
        }

        if (oper == VmVfNicFdbOperation.ADD) {
            logger.debug("add BridgeFdbEntryForVmNics hostNicMacs = " + hostNicMacs);
        } else {
            logger.debug("del BridgeFdbEntryForVmNics hostNicMacs = " + hostNicMacs);
        }

        // When pf is bond slave, we need to change physicalInterface from bond name to pf name.
        // And if there are more than one virtualized pf slave, we only choose the first one,
        // so there is a chance that vnic still cannot connect to vf.
        // The best practice is to make sure only one bond slave is splited into vfs.
        List<KVMHostAsyncHttpCallMsg> msgs = new ArrayList<>();
        for (String hostUuid : hostNicMacs.keySet()) {
            MevocoKVMAgentCommands.AddBridgeFdbEntryCmd cmd = new MevocoKVMAgentCommands.AddBridgeFdbEntryCmd();
            cmd.setMacs(hostNicMacs.get(hostUuid));
            if (cmd.getMacs().isEmpty()) {
                continue;
            }

            String pfName = VmVfNicUtils.getRandomPfNameInHostBonding(hostUuid, physicalInterface);
            if (pfName == null) {
                cmd.setPhysicalInterface(physicalInterface);
            } else {
                cmd.setPhysicalInterface(pfName);
            }

            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setNoStatusCheck(!hostStatusCheck);
            msg.setHostUuid(hostUuid);
            msg.setCommand(cmd);
            if (oper == VmVfNicFdbOperation.ADD) {
                msg.setPath(MevocoKVMConstant.ADD_BRIDGE_FDB_ENTRY_PATH);
            } else {
                msg.setPath(MevocoKVMConstant.DEL_BRIDGE_FDB_ENTRY_PATH);
            }
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
            msgs.add(msg);
        }

        new While<>(msgs).each((msg, cmpl) -> {
            bus.send(msg, new CloudBusCallBack(cmpl) {
                @Override
                public void run(MessageReply reply) {
                    MevocoKVMAgentCommands.AddBridgeFdbEntryCmd cmd = JSONObjectUtil.toObject(
                            msg.getCommand(), MevocoKVMAgentCommands.AddBridgeFdbEntryCmd.class);

                    if (!reply.isSuccess()) {
                        logger.error(String.format("failed to add bridge fdb in host[uuid:%s, nic:%s]: %s",
                                msg.getHostUuid(), cmd.getPhysicalInterface(), reply.getError()));
                    } else {
                        KVMHostAsyncHttpCallReply rly = reply.castReply();
                        MevocoKVMAgentCommands.AddBridgeFdbEntryRsp rsp = rly.toResponse(
                                MevocoKVMAgentCommands.AddBridgeFdbEntryRsp.class);
                        if (!rsp.isSuccess()) {
                            logger.error(String.format("failed to add bridge fdb in host[uuid:%s, nic:%s]: %s",
                                    msg.getHostUuid(), cmd.getPhysicalInterface(), rsp.getError()));
                        } else {
                            logger.debug(String.format("successfully added bridge fdb in host[uuid:%s, nic:%s]",
                                    msg.getHostUuid(), cmd.getPhysicalInterface()));
                        }
                    }

                    cmpl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    private String getSriovEnabledPhyNic(String l3Uuid, String hostUuid) {
        String l2NetworkUuid = Q.New(L3NetworkVO.class).select(L3NetworkVO_.l2NetworkUuid)
                .eq(L3NetworkVO_.uuid, l3Uuid).findValue();
        if (!vfPciDeviceUtils.isSriovEnabledOnL2Network(l2NetworkUuid)) {
            return "";
        }

        String phyNicName = VmVfNicUtils.getSriovPhysicalInterfaceName(hostUuid, l2NetworkUuid);
        if (phyNicName == null || phyNicName.isEmpty()) {
            return "";
        }

        return phyNicName;
    }

    private Map<String, List<VmNicInventory>> getSriovFdbInfoByHost(HostInventory host) {
        Map<String, List<VmNicInventory>> nicMaps = new HashMap<>();
        Map<String, String> l3SriovMap = new HashMap<>();

        if (!host.getHypervisorType().equals(KVMConstant.KVM_HYPERVISOR_TYPE)) {
            return nicMaps;
        }

        List<VmInstanceVO> vms = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.hostUuid, host.getUuid())
                .in(VmInstanceVO_.state, Arrays.asList(VmInstanceState.Running, VmInstanceState.Unknown)).list();

        for (VmInstanceVO vm : vms) {
            for (VmNicVO nic : vm.getVmNics()) {
                if (!nic.getType().equals(VmInstanceConstant.VIRTUAL_NIC_TYPE)) {
                    continue;
                }

                String l2NetworkUuid = Q.New(L3NetworkVO.class).select(L3NetworkVO_.l2NetworkUuid)
                        .eq(L3NetworkVO_.uuid, nic.getL3NetworkUuid()).findValue();
                if (!vfPciDeviceUtils.isSriovEnabledOnL2Network(l2NetworkUuid)) {
                    continue;
                }

                String phyNicName = l3SriovMap.get(nic.getL3NetworkUuid());
                if (phyNicName == null) {
                    phyNicName = getSriovEnabledPhyNic(nic.getL3NetworkUuid(), host.getUuid());
                    l3SriovMap.put(nic.getL3NetworkUuid(), phyNicName);
                }

                if (phyNicName.isEmpty()) {
                    continue;
                }

                nicMaps.computeIfAbsent(phyNicName, k -> new ArrayList<>()).add(VmNicInventory.valueOf(nic));
            }
        }

        return nicMaps;
    }

    private Map<String, List<VmNicInventory>> getSriovFdbInfoByVm(String hostUuid, VmInstanceInventory inv) {
        Map<String, List<VmNicInventory>> nicMaps = new HashMap<>();

        if (!KVMConstant.KVM_HYPERVISOR_TYPE.equals(inv.getHypervisorType())) {
            return nicMaps;
        }

        for (VmNicInventory nic : inv.getVmNics()) {
            if (!nic.getType().equals(VmInstanceConstant.VIRTUAL_NIC_TYPE)) {
                continue;
            }

            String l2NetworkUuid = Q.New(L3NetworkVO.class).select(L3NetworkVO_.l2NetworkUuid)
                    .eq(L3NetworkVO_.uuid, nic.getL3NetworkUuid()).findValue();
            if (!vfPciDeviceUtils.isSriovEnabledOnL2Network(l2NetworkUuid)) {
                continue;
            }

            String phyNicName = VmVfNicUtils.getSriovPhysicalInterfaceName(hostUuid, l2NetworkUuid);
            if (phyNicName == null || phyNicName.isEmpty()) {
                continue;
            }

            nicMaps.computeIfAbsent(phyNicName, k -> new ArrayList<>()).add(nic);
        }

        return nicMaps;
    }


    private Map<String, String> getSriovFdbInfoByVmNic(VmNicInventory nic) {
        Map<String, String> nicMaps = new HashMap<>();

        VmInstanceVO vmVo = dbf.findByUuid(nic.getVmInstanceUuid(), VmInstanceVO.class);
        if (vmVo == null) {
            return nicMaps;
        }

        VmInstanceInventory inv = VmInstanceInventory.valueOf(vmVo);
        if (inv.getHypervisorType() == null
                ||  !inv.getHypervisorType().equals(KVMConstant.KVM_HYPERVISOR_TYPE)) {
            return nicMaps;
        }

        if (!nic.getType().equals(VmInstanceConstant.VIRTUAL_NIC_TYPE)) {
            return nicMaps;
        }

        String l2NetworkUuid = Q.New(L3NetworkVO.class).select(L3NetworkVO_.l2NetworkUuid)
                .eq(L3NetworkVO_.uuid, nic.getL3NetworkUuid()).findValue();
        if (!vfPciDeviceUtils.isSriovEnabledOnL2Network(l2NetworkUuid)) {
            return nicMaps;
        }

        String phyNicName = VmVfNicUtils.getSriovPhysicalInterfaceName(inv.getHostUuid(), l2NetworkUuid);
        if (phyNicName == null || phyNicName.isEmpty()) {
            return nicMaps;
        }

        nicMaps.put(phyNicName, inv.getHostUuid());

        return nicMaps;
    }

    @Override
    public String preStartNewCreatedVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStartNewCreatedVm(VmInstanceInventory inv) {

    }

    @Override
    public void afterStartNewCreatedVm(VmInstanceInventory inv) {
        Map<String, List<VmNicInventory>> nicMaps = getSriovFdbInfoByVm(inv.getHostUuid(), inv);
        if (nicMaps.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<VmNicInventory>> entry : nicMaps.entrySet()) {
            String phyNic = entry.getKey();
            List<VmNicInventory> nics = entry.getValue();

            Map<String, List<String>> hostMap = new HashMap<>();
            hostMap.put(inv.getHostUuid(), nics.stream().map(VmNicInventory::getMac).collect(Collectors.toList()));
            changeBridgeFdbEntryForVmNics(hostMap, phyNic, VmVfNicFdbOperation.ADD, new NopeCompletion());
        }
    }

    @Override
    public void failedToStartNewCreatedVm(VmInstanceInventory inv, ErrorCode reason) {

    }

    @Override
    public String preStartVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStartVm(VmInstanceInventory inv) {

    }

    @Override
    public void afterStartVm(VmInstanceInventory inv) {
        Map<String, List<VmNicInventory>> nicMaps = getSriovFdbInfoByVm(inv.getHostUuid(), inv);
        if (nicMaps.isEmpty()) {
            return;
        }

        /* check if vm is started on shared storage */

        for (Map.Entry<String, List<VmNicInventory>> entry : nicMaps.entrySet()) {
            String phyNic = entry.getKey();
            List<VmNicInventory> nics = entry.getValue();

            Map<String, List<String>> hostMap = new HashMap<>();
            hostMap.put(inv.getHostUuid(), nics.stream().map(VmNicInventory::getMac).collect(Collectors.toList()));
            changeBridgeFdbEntryForVmNics(hostMap, phyNic, VmVfNicFdbOperation.ADD, new NopeCompletion());
        }
    }

    @Override
    public void failedToStartVm(VmInstanceInventory inv, ErrorCode reason) {

    }

    @Override
    public String preStopVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStopVm(VmInstanceInventory inv) {

    }

    @Override
    public void afterStopVm(VmInstanceInventory inv) {
        String hostUuid = inv.getHostUuid() != null ? inv.getHostUuid() : inv.getLastHostUuid();
        Map<String, List<VmNicInventory>> nicMaps = getSriovFdbInfoByVm(hostUuid, inv);
        if (nicMaps.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<VmNicInventory>> entry : nicMaps.entrySet()) {
            String phyNic = entry.getKey();
            List<VmNicInventory> nics = entry.getValue();

            Map<String, List<String>> hostMap = new HashMap<>();
            hostMap.put(hostUuid, nics.stream().map(VmNicInventory::getMac).collect(Collectors.toList()));
            changeBridgeFdbEntryForVmNics(hostMap, phyNic, VmVfNicFdbOperation.DELETE, new NopeCompletion());
        }
    }

    @Override
    public void failedToStopVm(VmInstanceInventory inv, ErrorCode reason) {

    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        Map<String, List<VmNicInventory>> nicMaps = getSriovFdbInfoByVm(srcHostUuid, inv);
        if (!nicMaps.isEmpty()) {
            for (Map.Entry<String, List<VmNicInventory>> entry : nicMaps.entrySet()) {
                String phyNic = entry.getKey();
                List<VmNicInventory> nics = entry.getValue();

                Map<String, List<String>> hostMap = new HashMap<>();
                hostMap.put(srcHostUuid, nics.stream().map(VmNicInventory::getMac).collect(Collectors.toList()));
                changeBridgeFdbEntryForVmNics(hostMap, phyNic, VmVfNicFdbOperation.DELETE, new NopeCompletion());
            }
        }


        nicMaps = getSriovFdbInfoByVm(inv.getHostUuid(), inv);
        if (nicMaps.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<VmNicInventory>> entry : nicMaps.entrySet()) {
            String phyNic = entry.getKey();
            List<VmNicInventory> nics = entry.getValue();

            Map<String, List<String>> hostMap = new HashMap<>();
            hostMap.put(inv.getHostUuid(), nics.stream().map(VmNicInventory::getMac).collect(Collectors.toList()));
            changeBridgeFdbEntryForVmNics(hostMap, phyNic, VmVfNicFdbOperation.ADD, new NopeCompletion());
        }
    }

    @Override
    public void afterAttachNicToVm(VmNicInventory nic) {
        Map<String, String> nicMaps = getSriovFdbInfoByVmNic(nic);
        if (nicMaps.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : nicMaps.entrySet()) {
            String phyNic = entry.getKey();
            String hostUuid = entry.getValue();

            Map<String, List<String>> hostMap = new HashMap<>();
            hostMap.put(hostUuid, Arrays.asList(nic.getMac()));
            changeBridgeFdbEntryForVmNics(hostMap, phyNic, VmVfNicFdbOperation.ADD, new NopeCompletion());
        }
    }

    @Override
    public void preDetachNic(VmNicInventory nic) {

    }

    @Override
    public void beforeDetachNic(VmNicInventory nic) {

    }

    @Override
    public void afterDetachNic(VmNicInventory nic) {
        Map<String, String> nicMaps = getSriovFdbInfoByVmNic(nic);
        if (nicMaps.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : nicMaps.entrySet()) {
            String phyNic = entry.getKey();
            String hostUuid = entry.getValue();

            Map<String, List<String>> hostMap = new HashMap<>();
            hostMap.put(hostUuid, Arrays.asList(nic.getMac()));
            changeBridgeFdbEntryForVmNics(hostMap, phyNic, VmVfNicFdbOperation.DELETE, new NopeCompletion());
        }
    }

    @Override
    public void failedToDetachNic(VmNicInventory nic, ErrorCode error) {

    }


    public void addFdbEntryPhysicalNic(PciDeviceInventory pci) {
        Map<String, List<String>> phyL2Map = VmVfNicUtils.getL2NetworkUuidsFromPciDevice(pci);
        if (phyL2Map.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<String>> entry : phyL2Map.entrySet()) {
            String phyNic = entry.getKey();
            List<String> l2Uuids = entry.getValue();

            List<String> l3Uuids = Q.New(L3NetworkVO.class).in(L3NetworkVO_.l2NetworkUuid, l2Uuids)
                    .select(L3NetworkVO_.uuid).listValues();
            if (l3Uuids.isEmpty()) {
                continue;
            }

            List<String> vmUuids = Q.New(VmInstanceVO.class).select(VmInstanceVO_.uuid)
                    .eq(VmInstanceVO_.hostUuid, pci.getHostUuid()).listValues();
            if (vmUuids.isEmpty()) {
                continue;
            }

            List<String> macs = Q.New(VmNicVO.class).select(VmNicVO_.mac).isNull(VmNicVO_.metaData).in(VmNicVO_.vmInstanceUuid, vmUuids)
                    .eq(VmNicVO_.type, VmInstanceConstant.VIRTUAL_NIC_TYPE).in(VmNicVO_.l3NetworkUuid, l3Uuids).listValues();
            if (macs.isEmpty()) {
                continue;
            }

            Map<String, List<String>> hostMap = new HashMap<>();
            hostMap.put(pci.getHostUuid(), macs);
            changeBridgeFdbEntryForVmNics(hostMap, phyNic, VmVfNicFdbOperation.ADD, new NopeCompletion());
        }
    }

    public void removeFdbEntryPhysicalNic(PciDeviceInventory pci) {
        Map<String, List<String>> phyL2Map = VmVfNicUtils.getL2NetworkUuidsFromPciDevice(pci);
        if (phyL2Map.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<String>> entry : phyL2Map.entrySet()) {
            String phyNic = entry.getKey();
            List<String> l2Uuids = entry.getValue();

            List<String> l3Uuids = Q.New(L3NetworkVO.class).in(L3NetworkVO_.l2NetworkUuid, l2Uuids)
                    .select(L3NetworkVO_.uuid).listValues();
            if (l3Uuids.isEmpty()) {
                continue;
            }

            List<String> vmUuids = Q.New(VmInstanceVO.class).select(VmInstanceVO_.uuid)
                    .eq(VmInstanceVO_.hostUuid, pci.getHostUuid()).listValues();
            if (vmUuids.isEmpty()) {
                continue;
            }

            List<String> macs = Q.New(VmNicVO.class).select(VmNicVO_.mac).isNull(VmNicVO_.metaData).in(VmNicVO_.vmInstanceUuid, vmUuids)
                    .eq(VmNicVO_.type, VmInstanceConstant.VIRTUAL_NIC_TYPE).in(VmNicVO_.l3NetworkUuid, l3Uuids).listValues();
            if (macs.isEmpty()) {
                continue;
            }

            Map<String, List<String>> hostMap = new HashMap<>();
            hostMap.put(pci.getHostUuid(), macs);
            changeBridgeFdbEntryForVmNics(hostMap, phyNic, VmVfNicFdbOperation.DELETE, new NopeCompletion());
        }
    }

    @Override
    public Flow createPostHostConnectFlow(HostInventory host) {
        return new NoRollbackFlow() {
            String __name__ = "update-vnic-macs-for-sr-iov";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                Map<String, List<VmNicInventory>> nicMaps = getSriovFdbInfoByHost(host);
                if (nicMaps.isEmpty()) {
                    logger.debug(String.format("update sr-iov fdb for host[uuid:%s, name:%s] successfully",
                            host.getUuid(), host.getName()));
                    trigger.next();
                    return;
                }

                for (Map.Entry<String, List<VmNicInventory>> entry : nicMaps.entrySet()) {
                    String phyNic = entry.getKey();
                    List<VmNicInventory> nics = entry.getValue();

                    Map<String, List<String>> hostMap = new HashMap<>();
                    hostMap.put(host.getUuid(), nics.stream().map(VmNicInventory::getMac).collect(Collectors.toList()));
                    changeBridgeFdbEntryForVmNics(hostMap, phyNic, VmVfNicFdbOperation.ADD, false, new Completion(trigger) {
                        @Override
                        public void success() {
                            logger.debug(String.format("update sr-iov fdb for host[uuid:%s, name:%s] successfully",
                                    host.getUuid(), host.getName()));
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.debug(String.format("update sr-iov fdb for host[uuid:%s, name:%s] failed, because %s",
                                    host.getUuid(), host.getName(), errorCode.getMessages()));
                            trigger.next();
                        }
                    });
                }
            }
        };
    }
}

package org.zstack.compute.sriov;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import org.zstack.compute.host.MevocoKVMAgentCommands.ChangeVfNicHaStateCmd;
import org.zstack.compute.host.MevocoKVMAgentCommands.ChangeVfNicHaStateRsp;
import org.zstack.compute.host.MevocoKVMConstant;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.sriov.VmVfNicConstant;
import org.zstack.header.sriov.VmVfNicInventory;
import org.zstack.header.sriov.VmVfNicVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.devices.DeviceAddress;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataManager;
import org.zstack.kvm.KVMAgentCommands.NicTO;
import org.zstack.kvm.KVMAgentCommands.VHostAddOn;
import org.zstack.kvm.KVMCompleteNicInformationExtensionPoint;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMHostFactory;
import org.zstack.pciDevice.PciDeviceVO;
import org.zstack.pciDevice.PciDeviceVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmVfNicChangeHaStateFlow extends NoRollbackFlow{
    private static final CLogger logger = Utils.getLogger(VmVfNicChangeHaStateFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected VmInstanceResourceMetadataManager vidm;
    @Autowired
    @Qualifier("KVMHostFactory")
    protected KVMHostFactory factory;

    @Transactional(readOnly = true)
    private NicTO completevfNicInfo(String vfUuid) {
        VmNicInventory nic = VmNicInventory.valueOf(dbf.findByUuid(vfUuid, VmNicVO.class));
        L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class));
        L2NetworkInventory l2Inv = L2NetworkInventory.valueOf(dbf.findByUuid(l3Inv.getL2NetworkUuid(), L2NetworkVO.class));

        KVMCompleteNicInformationExtensionPoint extp = factory.getCompleteNicInfoExtension(L2NetworkType.valueOf(l2Inv.getType()));
        NicTO to = extp.completeNicInformation(l2Inv, l3Inv, nic);

        to.setUuid(vfUuid);
        to.setNicInternalName(nic.getInternalName());
       
        VHostAddOn vHostAddOn = new VHostAddOn();
        vHostAddOn.setQueueNum(VmGlobalConfig.VM_NIC_MULTIQUEUE_NUM.defaultValue(Integer.class));
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

        DeviceAddress pci = vidm.getVmDeviceAddress(nic.getUuid(), nic.getVmInstanceUuid());
        if (pci != null) {
            to.setPci(pci);
        }

        VmVfNicInventory vfNic = VmVfNicInventory.valueOf(dbf.findByUuid(vfUuid, VmVfNicVO.class));
        String pciAddress = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.uuid, vfNic.getPciDeviceUuid())
                .select(PciDeviceVO_.pciDeviceAddress)
                .findValue();
        if (pciAddress == null) {
            throw new CloudRuntimeException(String.format("the pci address of vf nic[uuid:%s] is null", vfUuid));
        }
        to.setPciDeviceAddress(pciAddress);

        return to;
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        String haState = (String) data.get(VmVfNicConstant.Params.VmVfNicHaState.toString());
        VmVfNicInventory vfNic = (VmVfNicInventory) data.get(VmVfNicConstant.Params.VmVfNicInventory.toString());

        VmInstanceInventory vm = VmInstanceInventory.valueOf(dbf.findByUuid(vfNic.getVmInstanceUuid(), VmInstanceVO.class));
        String hostUuid = vm.getHostUuid();

        ChangeVfNicHaStateCmd cmd = new ChangeVfNicHaStateCmd();
        cmd.setVmUuid(vm.getUuid());
        cmd.setNic(completevfNicInfo(vfNic.getUuid()));
        cmd.setHaState(haState);
        
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(hostUuid);
        kmsg.setCommand(cmd);
        kmsg.setPath(MevocoKVMConstant.SET_VM_VF_NIC_STATE);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                } else {
                    KVMHostAsyncHttpCallReply ar = reply.castReply();
                    ChangeVfNicHaStateRsp rsp = ar.toResponse(ChangeVfNicHaStateRsp.class);
                    if (!rsp.isSuccess()) {
                        trigger.fail(operr("failed to change vf nic ha state, error: %s", rsp.getError()));
                        return;
                    }

                    trigger.next();
                }
            }
        });
    }
}

package org.zstack.vpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.compute.vm.VmInstanceManager;
import org.zstack.compute.vm.VmNicQosConfigBackend;
import org.zstack.compute.vm.VmNicQosStruct;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.SetNicQosOnKVMHostMsg;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.service.VirtualRouterHaCallbackInterface;
import org.zstack.header.network.service.VirtualRouterHaCallbackStruct;
import org.zstack.header.network.service.VirtualRouterHaGetCallbackExtensionPoint;
import org.zstack.header.network.service.VirtualRouterHaTask;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.vpc.ha.VpcHaGroupOperator;

import java.util.ArrayList;
import java.util.List;

public class VpcNicQosConfigBackend implements VmNicQosConfigBackend, VirtualRouterHaGetCallbackExtensionPoint {
    CLogger logger = Utils.getLogger(VpcNicQosConfigBackend.class);

    @Autowired
    VmInstanceManager vmMgr;
    @Autowired
    CloudBus bus;
    @Autowired
    VirtualRouterHaBackend haBackend;
    @Autowired
    DatabaseFacade dbf;

    private final String SET_VMNIC_QOS = "setNicQos";

    @Override
    public String getVmInstanceType() {
        return ApplianceVmConstant.APPLIANCE_VM_TYPE;
    }

    @Override
    public void addNicQos(String vmUuid, String vmNicUuid, Long outboundBandwidth, Long inboundBandwidth) {
        VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(VmInstanceConstant.USER_VM_TYPE);
        backend.addNicQos(vmUuid, vmNicUuid, outboundBandwidth, inboundBandwidth);

        /* save to backup router */
        String peerRouterUuid = VpcHaGroupOperator.getVpcHaRouterPeerUuid(vmUuid);
        if (peerRouterUuid == null) {
            return;
        }
        String l3Uuid = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).eq(VmNicVO_.uuid, vmNicUuid).findValue();
        VirtualRouterVmVO peerRouter = dbf.findByUuid(peerRouterUuid, VirtualRouterVmVO.class);
        VmNicVO peerNic = null;
        for (VmNicVO nic : peerRouter.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(l3Uuid)) {
                peerNic = nic;
                break;
            }
        }
        if (peerNic == null) {
            logger.warn(String.format("set VPC Nic to router [uuid:%s] failed, because there is no nic in l3 network[uuid:%s]",
                    peerRouterUuid, l3Uuid));
            return;
        }
        backend.addNicQos(peerRouterUuid, peerNic.getUuid(), outboundBandwidth, inboundBandwidth);

        if (peerRouter.getHostUuid() == null) {
            logger.warn(String.format("set VPC Nic to router [uuid:%s] failed, because there is no hostUuid",
                    peerRouterUuid));
            return;
        }

        /* submit a gc job to  add to host */
        VmNicQosStruct struct = new VmNicQosStruct();
        struct.hostUuid = peerRouter.getHostUuid();
        struct.vmUuid = peerRouter.getUuid();
        struct.vmNicUuid = peerNic.getUuid();
        struct.internalName = peerNic.getInternalName();
        struct.inboundBandwidth = inboundBandwidth;
        struct.outboundBandwidth = outboundBandwidth;

        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(SET_VMNIC_QOS);
        task.setOriginRouterUuid(vmUuid);
        task.setJsonData(JSONObjectUtil.toJsonString(struct));
        haBackend.submitVirtualRouterHaTask(task, new NopeCompletion());
    }

    @Override
    public void deleteNicQos(String vmUuid, String vmNicUuid, String direction) {
        VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(VmInstanceConstant.USER_VM_TYPE);
        backend.deleteNicQos(vmUuid, vmNicUuid, direction);

        /* delete from backup router */
        String peerRouterUuid = VpcHaGroupOperator.getVpcHaRouterPeerUuid(vmUuid);
        if (peerRouterUuid == null) {
            return;
        }

        String l3Uuid = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).eq(VmNicVO_.uuid, vmNicUuid).findValue();
        VirtualRouterVmVO peerRouter = dbf.findByUuid(peerRouterUuid, VirtualRouterVmVO.class);
        VmNicVO peerNic = null;
        for (VmNicVO nic : peerRouter.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(l3Uuid)) {
                peerNic = nic;
                break;
            }
        }
        if (peerNic == null) {
            logger.warn(String.format("delete VPC nic qos from router [uuid:%s] failed, because there is no nic in l3 network[uuid:%s]",
                    peerRouterUuid, l3Uuid));
            return;
        }
        backend.deleteNicQos(peerRouterUuid, peerNic.getUuid(), direction);
        if (peerRouter.getHostUuid() == null) {
            logger.warn(String.format("delete VPC nic qos from router [uuid:%s] failed, because there is no hostUuid",
                    peerRouterUuid));
            return;
        }

        /* submit a gc job to configure host */
        VmNicQosStruct struct = new VmNicQosStruct();
        struct.hostUuid = peerRouter.getHostUuid();
        struct.vmUuid = peerRouter.getUuid();
        struct.vmNicUuid = peerNic.getUuid();
        struct.internalName = peerNic.getInternalName();
        if (direction.equals("in")) {
            struct.inboundBandwidth = 0L;
        } else if (direction.equals("out")){
            struct.outboundBandwidth = 0L;
        }

        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(SET_VMNIC_QOS);
        task.setOriginRouterUuid(vmUuid);
        task.setJsonData(JSONObjectUtil.toJsonString(struct));
        haBackend.submitVirtualRouterHaTask(task, new NopeCompletion());
    }

    @Override
    public VmNicQosStruct getNicQos(String vmUuid, String vmNicUuid) {
        VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(VmInstanceConstant.USER_VM_TYPE);
        VmNicQosStruct struct = backend.getNicQos(vmUuid, vmNicUuid);
        if (struct.inboundBandwidth != -1L && struct.outboundBandwidth != -1L) {
            return struct;
        }

        /* get from backup router */
        String peerRouterUuid = VpcHaGroupOperator.getVpcHaRouterPeerUuid(vmUuid);
        if (peerRouterUuid == null) {
            return struct;
        }

        String l3Uuid = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).eq(VmNicVO_.uuid, vmNicUuid).findValue();
        VirtualRouterVmVO peerRouter = dbf.findByUuid(peerRouterUuid, VirtualRouterVmVO.class);
        VmNicVO peerNic = null;
        for (VmNicVO nic : peerRouter.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(l3Uuid)) {
                peerNic = nic;
                break;
            }
        }
        if (peerNic == null) {
            return struct;
        }
        return backend.getNicQos(peerRouterUuid, peerNic.getUuid());
    }

    @Override
    public List<VirtualRouterHaCallbackStruct> getCallback() {
        List<VirtualRouterHaCallbackStruct> structs = new ArrayList<>();

        VirtualRouterHaCallbackStruct setVmNicQos = new VirtualRouterHaCallbackStruct();
        setVmNicQos.type = SET_VMNIC_QOS;
        setVmNicQos.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VmNicQosStruct struct = JSONObjectUtil.toObject(task.getJsonData(), VmNicQosStruct.class);
                SetNicQosOnKVMHostMsg hmsg = new SetNicQosOnKVMHostMsg();
                hmsg.setInternalName(struct.internalName);
                hmsg.setInboundBandwidth(struct.inboundBandwidth);
                hmsg.setOutboundBandwidth(struct.outboundBandwidth);
                hmsg.setVmUuid(struct.vmUuid);
                hmsg.setHostUuid(struct.hostUuid);

                bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, hmsg.getHostUuid());
                bus.send(hmsg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                        } else {
                            completion.success();
                        }
                    }
                });
            }
        };
        structs.add(setVmNicQos);

        return structs;
    }

    @Override
    public void addVmQos(String vmUuid, Long outboundBandwidth, Long inboundBandwidth) {
        /* because virtual router offering doesn't has nic qos, so do nothing here */
    }

    @Override
    public void deleteVmQos(String vmUuid, String direction) {
        /* because virtual router offering doesn't has nic qos, so do nothing here */
    }

    @Override
    public VmNicQosStruct getVmQos(String vmUuid) {
        VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(VmInstanceConstant.USER_VM_TYPE);
        return backend.getVmQos(vmUuid);
    }
}

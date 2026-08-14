package org.zstack.compute.vdpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vdpa.GenerateVdpaMsg;
import org.zstack.header.vdpa.DeleteVdpasMsg;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

public class VmVdpaNicKvmBackend implements VmVdpaNicHypervisorBackend{
    private static final CLogger logger = Utils.getLogger(VmVdpaNicKvmBackend.class);

    @Autowired
    private CloudBus bus;

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public void expungeVdpas(String hostUuid, DeleteVdpasMsg msgs, Completion completion) {
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        KVMAgentCommands.DeleteVdpaCmd cmd = new KVMAgentCommands.DeleteVdpaCmd();
        String vmUuid = msgs.getVmInstanceUuid();
        cmd.vmUuid = vmUuid;
        if (msgs.getNic() != null) {
            cmd.setNicInternalName(msgs.getNic().getInternalName());
        }
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setPath(KVMConstant.KVM_DELETE_VDPA_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(operr("failed to delete vdpas in host[uuid:%s] for vm[uuid:%s] : %s",
                            hostUuid, vmUuid, reply.getError()));
                    return;
                } else {
                    KVMHostAsyncHttpCallReply rly = reply.castReply();
                    KVMAgentCommands.DeleteVdpaRsp rsp = rly.toResponse(
                            KVMAgentCommands.DeleteVdpaRsp.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(operr("failed to delete vdpas in host[uuid:%s] for vm[uuid:%s] : %s",
                                hostUuid, vmUuid, rsp.getError()));
                        return;
                    } else {
                        logger.debug(String.format("successfully delete vdpas in host[uuid:%s] for vm[uuid:%s].",
                                hostUuid, vmUuid));
                    }
                }

                completion.success();
            }
        });
    }

    @Override
    public void generateVdpa(String hostUuid, GenerateVdpaMsg msgs, Completion completion) {
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        KVMAgentCommands.GenerateVdpaCmd cmd = new KVMAgentCommands.GenerateVdpaCmd();
        String vmUuid = msgs.getVmInstanceUuid();
        cmd.vmUuid = vmUuid;
        cmd.setNics(msgs.getNics());
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setPath(KVMConstant.KVM_GENERATE_VDPA_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(operr("failed to generate vdpas in host[uuid:%s] for vm[uuid:%s] : %s",
                            hostUuid, vmUuid, reply.getError()));
                    return;
                } else {
                    KVMHostAsyncHttpCallReply rly = reply.castReply();
                    KVMAgentCommands.GenerateVdpaResponse rsp = rly.toResponse(
                            KVMAgentCommands.GenerateVdpaResponse.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(operr("failed to generate vdpas in host[uuid:%s] for vm[uuid:%s] : %s",
                                hostUuid, vmUuid, rsp.getError()));
                        return;
                    } else {
                        logger.debug(String.format("successfully generate vdpas in host[uuid:%s] for vm[uuid:%s].",
                                hostUuid, vmUuid));
                    }
                }

                completion.success();
            }
        });
    }
}

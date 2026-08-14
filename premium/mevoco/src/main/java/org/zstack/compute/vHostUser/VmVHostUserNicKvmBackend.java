package org.zstack.compute.vHostUser;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vHostUser.DeleteVHostUserResourceMsg;
import org.zstack.header.vHostUser.GenerateVHostUserResourceMsg;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

public class VmVHostUserNicKvmBackend implements VmVHostUserNicHypervisorBackend{
    private static final CLogger logger = Utils.getLogger(VmVHostUserNicKvmBackend.class);

    @Autowired
    private CloudBus bus;

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public void expungeVHostUserResource(String hostUuid, DeleteVHostUserResourceMsg msgs, Completion completion) {
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        KVMAgentCommands.DeleteVHostUserClientCmd cmd = new KVMAgentCommands.DeleteVHostUserClientCmd();
        String vmUuid = msgs.getVmInstanceUuid();
        cmd.vmUuid = vmUuid;
        if (msgs.getNic() != null) {
            cmd.setNicInternalName(msgs.getNic().getInternalName());
        }
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setPath(KVMConstant.KVM_DELETE_VHOST_USER_CLIENT_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(operr("failed to delete vHost User Client in host[uuid:%s] for vm[uuid:%s] : %s",
                            hostUuid, vmUuid, reply.getError()));
                    return;
                } else {
                    KVMHostAsyncHttpCallReply rly = reply.castReply();
                    KVMAgentCommands.DeleteVHostUserClientRsp rsp = rly.toResponse(KVMAgentCommands.DeleteVHostUserClientRsp.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(operr("failed to delete vHost User Client in host[uuid:%s] for vm[uuid:%s] : %s",
                                hostUuid, vmUuid, rsp.getError()));
                        return;
                    } else {
                        logger.debug(String.format("successfully delete vHost User Client in host[uuid:%s] for vm[uuid:%s].",
                                hostUuid, vmUuid));
                    }
                }

                completion.success();
            }
        });
    }

    @Override
    public void generateVHostUserResource(String hostUuid, GenerateVHostUserResourceMsg msgs, Completion completion) {
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        KVMAgentCommands.GenerateVHostUserClientCmd cmd = new KVMAgentCommands.GenerateVHostUserClientCmd();
        String vmUuid = msgs.getVmInstanceUuid();
        cmd.vmUuid = vmUuid;
        cmd.setNics(msgs.getNics());
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setPath(KVMConstant.KVM_GENERATE_VHOST_USER_CLIENT_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(operr("failed to generate vHost User Client in host[uuid:%s] for vm[uuid:%s] : %s",
                            hostUuid, vmUuid, reply.getError()));
                    return;
                } else {
                    KVMHostAsyncHttpCallReply rly = reply.castReply();
                    KVMAgentCommands.GenerateVHostUserClientResponse rsp = rly.toResponse(
                            KVMAgentCommands.GenerateVHostUserClientResponse.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(operr("failed to generate vHost User Client in host[uuid:%s] for vm[uuid:%s] : %s",
                                hostUuid, vmUuid, rsp.getError()));
                        return;
                    } else {
                        logger.debug(String.format("successfully generate vHost User Client in host[uuid:%s] for vm[uuid:%s].",
                                hostUuid, vmUuid));
                    }
                }

                completion.success();
            }
        });
    }
}

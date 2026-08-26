package org.zstack.storage.encrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;

import static org.zstack.core.Platform.operr;

public class ZbsVolumeEncryptionKvmCaller {
    @Autowired
    private CloudBus bus;

    <T extends KVMAgentCommands.AgentResponse> void call(String hostUuid, String path,
                                                         KVMAgentCommands.AgentCommand cmd,
                                                         Class<T> rspClass,
                                                         String operation,
                                                         ReturnValueCompletion<T> completion) {
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setCommand(cmd);
        kmsg.setPath(path);
        kmsg.setHostUuid(hostUuid);
        kmsg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply kr = reply.castReply();
                T rsp = kr.toResponse(rspClass);
                if (rsp == null) {
                    completion.fail(operr("kvm host[uuid:%s] returned null reply for zbs %s path[%s]",
                            hostUuid, operation, path));
                    return;
                }
                if (!rsp.isSuccess()) {
                    completion.fail(operr("kvm host[uuid:%s] zbs %s path[%s] failed, because: %s",
                            hostUuid, operation, path, rsp.getError()));
                    return;
                }

                completion.success(rsp);
            }
        });
    }
}

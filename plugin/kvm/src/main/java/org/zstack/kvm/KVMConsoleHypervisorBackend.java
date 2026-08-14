package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.console.ConsoleHypervisorBackend;
import org.zstack.header.console.ConsoleUrl;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.KVMAgentCommands.GetVncPortResponse;

import java.net.URI;
import java.net.URISyntaxException;

import static org.zstack.core.Platform.inerr;
import static org.zstack.core.Platform.operr;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class KVMConsoleHypervisorBackend implements ConsoleHypervisorBackend {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public HypervisorType getConsoleBackendHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public void generateConsoleUrl(final VmInstanceInventory vm, final ReturnValueCompletion<ConsoleUrl> complete) {
        KVMAgentCommands.GetVncPortCmd cmd = new KVMAgentCommands.GetVncPortCmd();
        cmd.setVmUuid(vm.getUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(vm.getHostUuid());
        msg.setCommand(cmd);
        msg.setNoStatusCheck(true);
        msg.setPath(KVMConstant.KVM_GET_VNC_PORT_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, vm.getHostUuid());
        bus.send(msg, new CloudBusCallBack(complete) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    complete.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply kreply = reply.castReply();
                GetVncPortResponse rsp = kreply.toResponse(GetVncPortResponse.class);
                if (!rsp.isSuccess()) {
                    complete.fail(operr("failed to get VNC port from VM[%s]", vm.getUuid())
                            .withOpaque("response.error", rsp.getError()));
                    return;
                }

                if (rsp.getPort() < 0) {
                    complete.fail(operr("unexpected VNC port number[%d] for VM [uuid:%s]", rsp.getPort(), vm.getUuid()));
                    return;
                }

                String mgmtIp = Q.New(HostVO.class)
                        .eq(HostVO_.uuid, vm.getHostUuid())
                        .select(HostVO_.managementIp)
                        .findValue();
                try {
                    // see https://tools.ietf.org/html/rfc7869#section-2.1
                    URI uri = new URI(String.format("vnc://%s:%s/", mgmtIp, rsp.getPort()));
                    ConsoleUrl consoleUrl = new ConsoleUrl();
                    consoleUrl.setUri(uri);
                    consoleUrl.setVersion(dbf.getDbVersion());
                    complete.success(consoleUrl);
                } catch (URISyntaxException e) {
                    complete.fail(inerr(e.getMessage()));
                }
            }
        });
    }
}

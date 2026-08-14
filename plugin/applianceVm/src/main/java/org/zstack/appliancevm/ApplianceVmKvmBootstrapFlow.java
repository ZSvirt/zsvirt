package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.ApplianceVmKvmCommands.PrepareBootstrapInfoRsp;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;

import static org.zstack.core.Platform.operr;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ApplianceVmKvmBootstrapFlow extends NoRollbackFlow {
    @Autowired
    private ApplianceVmFacade apvmf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ApplianceVmKvmBackend kvmExt;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        Map<String, Object> info = apvmf.prepareBootstrapInformation(spec);
        ApplianceVmKvmCommands.PrepareBootstrapInfoCmd cmd = new ApplianceVmKvmCommands.PrepareBootstrapInfoCmd();
        cmd.setInfo(info);
        cmd.setSocketPath(kvmExt.makeChannelSocketPath(spec.getVmInventory().getUuid()));
        cmd.setBootStrapInfoTimeout(ApplianceVmGlobalConfig.BOOTSTRAPINFO_TIMEOUT.value(Integer.class));

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setPath(cmd.PATH);
        msg.setCommand(cmd);
        msg.setHostUuid(spec.getDestHost().getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, spec.getDestHost().getUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    chain.fail(reply.getError());
                } else {
                    KVMHostAsyncHttpCallReply hreply = reply.castReply();
                    PrepareBootstrapInfoRsp rsp = hreply.toResponse(PrepareBootstrapInfoRsp.class);
                    if (rsp.isSuccess()) {
                        chain.next();
                    } else {
                        chain.fail(operr("set appliance bootstrapinfo error, because:%s", rsp.getError()));
                    }
                }
            }
        });
    }
}

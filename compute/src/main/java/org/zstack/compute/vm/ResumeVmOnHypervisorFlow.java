package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.vm.ResumeVmOnHypervisorMsg;
import org.zstack.header.vm.VmInstance;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.Map;

/**
 * Created by luchukun on 10/29/16.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ResumeVmOnHypervisorFlow extends NoRollbackFlow{
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;

    @Override
    public void run(final FlowTrigger chain, Map data){
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        ResumeVmOnHypervisorMsg msg = new ResumeVmOnHypervisorMsg();
        msg.setVmInventory(spec.getVmInventory());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID,spec.getVmInventory().getHostUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    chain.next();
                    return;
                }

                chain.fail(reply.getError());
            }
        });
    }
}
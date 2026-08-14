package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmUpdateNicOnHypervisorFlow extends NoRollbackFlow {

    @Autowired
    private CloudBus bus;

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        HostInventory dest = spec.getDestHost();
        VmInstanceInventory vm = spec.getVmInventory();

        /* there is no host information, trigger next */
        if (dest == null) {
            trigger.next();
            return;
        }

        VmUpdateNicOnHypervisorMsg cmsg = new VmUpdateNicOnHypervisorMsg();
        cmsg.setVmInstanceUuid(vm.getUuid());
        cmsg.setHostUuid(dest.getUuid());
        bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, vm.getUuid());
        bus.send(cmsg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                    return;
                }

                trigger.next();
            }
        });
    }
}

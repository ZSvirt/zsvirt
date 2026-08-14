package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.DetachIsoOnHypervisorMsg;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.Map;

/**
 * Created by frank on 10/17/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DetachIsoOnHypervisorFlow extends NoRollbackFlow {
    @Autowired
    private CloudBus bus;

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final String isoUuid = (String) data.get(VmInstanceConstant.Params.DetachingIsoUuid.toString());

        DetachIsoOnHypervisorMsg msg = new DetachIsoOnHypervisorMsg();
        msg.setHostUuid(spec.getDestHost().getUuid());
        msg.setVmInstanceUuid(spec.getVmInventory().getUuid());
        msg.setIsoUuid(isoUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
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

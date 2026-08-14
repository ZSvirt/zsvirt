package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.AttachIsoOnHypervisorMsg;
import org.zstack.header.host.HostConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.Map;

/**
 * Created by frank on 10/17/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AttachIsoOnHypervisorFlow extends NoRollbackFlow {
    @Autowired
    private CloudBus bus;

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final ImageInventory iso = (ImageInventory) data.get(VmInstanceConstant.Params.AttachingIsoInventory.toString());
        final VmInstanceSpec.IsoSpec isoSpec = spec.getDestIsoList().stream()
                .filter(s -> s.getImageUuid().equals(iso.getUuid()))
                .findAny()
                .orElse(null);

        AttachIsoOnHypervisorMsg msg = new AttachIsoOnHypervisorMsg();
        msg.setHostUuid(spec.getDestHost().getUuid());
        msg.setIsoSpec(isoSpec);
        msg.setVmInstanceUuid(spec.getVmInventory().getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, spec.getDestHost().getUuid());
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

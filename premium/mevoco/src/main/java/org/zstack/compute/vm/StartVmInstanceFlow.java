package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.StartVmInstanceMsg;
import org.zstack.header.vm.StartVmInstanceReply;
import org.zstack.header.vm.VmInstanceConstant;

import java.util.Collection;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class StartVmInstanceFlow extends NoRollbackFlow {
    @Autowired
    private CloudBus bus;

    @Override
    public boolean skip(Map data) {
        return ((Collection) data.get(PremiumVmInstanceConstant.DATA_VOLUME_SNAPSHOT)).isEmpty();
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        CreateVmFromVolumeResourceSpec spec = (CreateVmFromVolumeResourceSpec) data.get(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC);

        StartVmInstanceMsg smsg = new StartVmInstanceMsg();
        smsg.setVmInstanceUuid(spec.getVmInstance().getUuid());
        if (spec.getApiMsg().getClusterUuid() != null) {
            smsg.setClusterUuid(spec.getApiMsg().getClusterUuid());
        }
        if (spec.getApiMsg().getHostUuid() != null) {
            smsg.setHostUuid(spec.getVmInstance().getLastHostUuid());
        }
        bus.makeTargetServiceIdByResourceUuid(smsg, VmInstanceConstant.SERVICE_ID, smsg.getVmInstanceUuid());
        bus.send(smsg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    StartVmInstanceReply r = reply.castReply();
                    spec.setVmInstance(r.getInventory());
                } else if (CoreGlobalProperty.UNIT_TEST_ON) {
                    trigger.fail(reply.getError());
                    return;
                }
                trigger.next();
            }
        });
    }
}

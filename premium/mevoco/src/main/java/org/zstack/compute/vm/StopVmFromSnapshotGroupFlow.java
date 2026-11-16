package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.StopVmInstanceMsg;
import org.zstack.header.vm.StopVmInstanceReply;
import org.zstack.header.vm.VmCreationStrategy;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class StopVmFromSnapshotGroupFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(StopVmFromSnapshotGroupFlow.class);

    @Autowired
    private CloudBus bus;

    @Override
    public boolean skip(Map data) {
        CreateVmFromVolumeResourceSpec spec = (CreateVmFromVolumeResourceSpec) data.get(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC);
        return !VmCreationStrategy.CreateStopped.toString().equals(spec.getApiMsg().getStrategy())
                || !VmInstanceState.Running.toString().equals(spec.getVmInstance().getState());
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        CreateVmFromVolumeResourceSpec spec = (CreateVmFromVolumeResourceSpec) data.get(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC);
        StopVmInstanceMsg msg = new StopVmInstanceMsg();
        msg.setVmInstanceUuid(spec.getVmInstance().getUuid());
        msg.setStopHA(true);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to stop VM[uuid:%s] after snapshot group creation: %s",
                            msg.getVmInstanceUuid(), reply.getError()));
                    trigger.next();
                    return;
                }

                StopVmInstanceReply stopReply = reply.castReply();
                spec.setVmInstance(stopReply.getInventory());
                trigger.next();
            }
        });
    }
}

package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.CheckL2NetworkOnHostMsg;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmMigrationCheckL2NetworkOnHostFlow implements Flow {
    @Autowired
    private CloudBus bus;

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        boolean allowUnknown = spec.getMessage() instanceof MigrateVmMessage && ((MigrateVmMessage) spec.getMessage()).isAllowUnknown();

        List<CheckL2NetworkOnHostMsg> cmsgs = new ArrayList<CheckL2NetworkOnHostMsg>();
        for (L3NetworkInventory l3 : VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks())) {
            CheckL2NetworkOnHostMsg msg = new CheckL2NetworkOnHostMsg();
            msg.setL2NetworkUuid(l3.getL2NetworkUuid());
            msg.setHostUuid(spec.getDestHost().getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, l3.getL2NetworkUuid());
            cmsgs.add(msg);
        }

        if (cmsgs.isEmpty()) {
            trigger.next();
            return;
        }

        bus.send(cmsgs, new CloudBusListCallBack(trigger) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    if (!r.isSuccess() && !allowUnknown) {
                        trigger.fail(r.getError());
                        return;
                    }
                }

                trigger.next();
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        trigger.rollback();
    }
}

package org.zstack.mevoco;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.ha.VmHAExecutor;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterPostCreateFlowExtensionPoint;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.vyos.VyosPostCreateFlowExtensionPoint;

import java.util.Map;

/**
 * Created by xing5 on 2016/11/14.
 */
public class VirtualRouterSetHaLevelExtension implements VirtualRouterPostCreateFlowExtensionPoint, VyosPostCreateFlowExtensionPoint {
    @Autowired
    VmHAExecutor vmHAExecutor;

    private Flow getSetHaLevelFlow() {
        return new NoRollbackFlow() {
            String __name__ = "set-ha-level";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());

                vmHAExecutor.setHALevelForVM(vr.getUuid())
                        .toNeverStop()
                        .update();

                trigger.next();
            }
        };
    }

    @Override
    public Flow vyosPostCreateFlow() {
        return getSetHaLevelFlow();
    }

    @Override
    public Flow virtualRouterPostCreateFlow() {
        return getSetHaLevelFlow();
    }
}

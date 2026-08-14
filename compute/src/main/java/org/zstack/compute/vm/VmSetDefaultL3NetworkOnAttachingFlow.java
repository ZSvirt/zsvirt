package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by frank on 7/19/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmSetDefaultL3NetworkOnAttachingFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmSetDefaultL3NetworkOnAttachingFlow.class);
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        if (spec.getVmInventory().getDefaultL3NetworkUuid() != null) {
            trigger.next();
            return;
        }

        L3NetworkInventory l3 = VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks()).get(0);
        VmInstanceVO vm = dbf.findByUuid(spec.getVmInventory().getUuid(), VmInstanceVO.class);
        vm.setDefaultL3NetworkUuid(l3.getUuid());
        dbf.update(vm);

        data.put(VmSetDefaultL3NetworkOnAttachingFlow.class, true);
        trigger.next();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        if (data.containsKey(VmSetDefaultL3NetworkOnAttachingFlow.class)) {
            VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
            VmInstanceVO vm = dbf.findByUuid(spec.getVmInventory().getUuid(), VmInstanceVO.class);
            vm.setDefaultL3NetworkUuid(null);
            dbf.update(vm);
        }

        trigger.rollback();
    }
}

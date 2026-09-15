package org.zstack.allocator.domain.compute.flows;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.allocator.domain.compute.evaluator.AllocatorDomainPrimaryStorageGate;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DomainFilterPrimaryStorageFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(DomainFilterPrimaryStorageFlow.class);

    @Autowired(required = false)
    private AllocatorDomainPrimaryStorageGate gate;

    @Override
    @SuppressWarnings("rawtypes")
    public void run(FlowTrigger trigger, Map data) {
        if (gate == null) {
            logger.debug("skip DomainFilterPrimaryStorageFlow: AllocatorDomainPrimaryStorageGate bean is not loaded");
            trigger.next();
            return;
        }
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        ErrorCode error = gate.filterRootVolume(spec);
        if (error != null) {
            trigger.fail(error);
            return;
        }
        trigger.next();
    }
}

package org.zstack.allocator.domain.compute.flows;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.allocator.domain.compute.evaluator.AllocatorDomainPrimaryStorageGate;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DomainFilterPrimaryStorageFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(DomainFilterPrimaryStorageFlow.class);

    @Autowired(required = false)
    private AllocatorDomainPrimaryStorageGate gate;

    @Override
    @SuppressWarnings({"unchecked"})
    public void run(FlowTrigger trigger, Map<String, Object> data) {
        if (gate == null) {
            logger.warn("skip DomainFilterPrimaryStorageFlow: AllocatorDomainPrimaryStorageGate bean is not loaded");
            trigger.next();
            return;
        }
        PrimaryStorageAllocationSpec spec = (PrimaryStorageAllocationSpec) data.get(PrimaryStorageConstant.AllocatorParams.SPEC);
        List<PrimaryStorageVO> candidates = (List<PrimaryStorageVO>) data.get(PrimaryStorageConstant.AllocatorParams.CANDIDATES);
        ErrorCode error = gate.filterRootVolume(spec, candidates);
        if (error != null) {
            trigger.fail(error);
            return;
        }
        data.put(PrimaryStorageConstant.AllocatorParams.CANDIDATES, candidates);
        trigger.next();
    }
}

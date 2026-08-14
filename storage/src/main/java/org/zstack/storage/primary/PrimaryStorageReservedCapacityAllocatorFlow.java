package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.header.storage.primary.PrimaryStorageConstant.AllocatorParams;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.SizeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageReservedCapacityAllocatorFlow extends NoRollbackFlow {

    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected PrimaryStorageOverProvisioningManager psRatioMgr;
    @Autowired
    protected PrimaryStoragePhysicalCapacityManager physicalCapacityMgr;
    
    @Override
    public void run(FlowTrigger trigger, Map data) {
        PrimaryStorageAllocationSpec spec = (PrimaryStorageAllocationSpec) data.get(AllocatorParams.SPEC);
        List<PrimaryStorageVO> candidates = (List<PrimaryStorageVO>) data.get(AllocatorParams.CANDIDATES);
        DebugUtils.Assert(candidates != null && !candidates.isEmpty(), "PrimaryStorageReservedCapacityAllocatorFlow cannot be the first element in allocator chain");

        List<PrimaryStorageVO> ret = candidates.stream().filter(psvo ->
                PrimaryStorageCapacityChecker.New(psvo.getCapacity())
                .checkIncreasedAndTotalRequiredSize(spec.getSize(), spec.getTotalSize())
        ).collect(Collectors.toList());

        if (ret.isEmpty()) {
            throw new OperationFailureException(operr("after subtracting reserved capacity[%s], there is no primary storage having required size[%s bytes], may be the threshold of primary storage physical capacity setting is lower",
                            PrimaryStorageGlobalConfig.RESERVED_CAPACITY.value(), spec.getSize()));
        }

        data.put(AllocatorParams.CANDIDATES, ret);
        trigger.next();
    }
}


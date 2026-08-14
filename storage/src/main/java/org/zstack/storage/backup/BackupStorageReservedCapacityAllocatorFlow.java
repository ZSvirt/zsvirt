package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.backup.BackupStorageAllocationSpec;
import org.zstack.header.storage.backup.BackupStorageConstant.AllocatorParams;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.vo.ResourceVO;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.SizeUtils;

import static org.zstack.core.Platform.operr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BackupStorageReservedCapacityAllocatorFlow extends NoRollbackFlow {
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    @Transactional(readOnly = true)
    public void run(FlowTrigger trigger, Map data) {
        BackupStorageAllocationSpec spec = (BackupStorageAllocationSpec) data.get(AllocatorParams.SPEC);
        List<BackupStorageVO> candidates = (List<BackupStorageVO>) data.get(AllocatorParams.CANDIDATES);
        DebugUtils.Assert(candidates != null && !candidates.isEmpty(), "BackupStorageReservedCapacityAllocatorFlow cannot be the first element in the allocator chain");

        List<String> backupStorageUuids = candidates.stream().map(ResourceVO::getUuid).collect(Collectors.toList());
        Map<String, String> resourceConfigValueMap = rcf.getResourceConfigValues(
                BackupStorageGlobalConfig.RESERVED_CAPACITY, backupStorageUuids, String.class);

        List<BackupStorageVO> ret = new ArrayList<>();
        for (BackupStorageVO vo : candidates) {
            long reservedCapacity = SizeUtils.sizeStringToBytes(resourceConfigValueMap.get(vo.getUuid()));
            if (vo.getAvailableCapacity() - reservedCapacity > spec.getSize()) {
                ret.add(vo);
            }
        }

        if (ret.isEmpty()) {
            throw new OperationFailureException(operr("after subtracting reserved capacity, no backup storage has required capacity[%s bytes]", spec.getSize()));
        }

        data.put(AllocatorParams.CANDIDATES, ret);
        trigger.next();
    }
}

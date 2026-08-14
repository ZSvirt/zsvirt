package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by MaJin on 2019/3/4.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageSortFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(PrimaryStorageSortFlow.class);

    @Autowired
    protected PrimaryStoragePriorityGetter priorityGetter;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        PrimaryStorageAllocationSpec spec = (PrimaryStorageAllocationSpec) data.get(PrimaryStorageConstant.AllocatorParams.SPEC);
        if (spec.getImageUuid() == null) {
            trigger.next();
            return;
        }

        List<PrimaryStorageVO> candidates = (List<PrimaryStorageVO>) data.get(PrimaryStorageConstant.AllocatorParams.CANDIDATES);

        if (candidates.size() == 1) {
            trigger.next();
            return;
        }

        PrimaryStoragePriorityGetter.PrimaryStoragePriority result = priorityGetter
                .getPrimaryStoragePriority(spec.getImageUuid(), spec.getBackupStorageUuid());
        Map<String, Integer> priority = result.psPriority.stream().collect(Collectors.toMap(it -> it.PS, it -> it.priority));
        candidates.sort(Comparator.comparing(it -> priority.getOrDefault(it.getType(), result.defaultPriority)));
        trigger.next();
    }
}

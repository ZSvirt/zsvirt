package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;

import java.util.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageSortByAvailableVirtualCapacityFlow extends NoRollbackFlow {
    @Override
    public void run(FlowTrigger trigger, Map data) {
        List<PrimaryStorageVO> candidates = (List<PrimaryStorageVO>) data.get(PrimaryStorageConstant.AllocatorParams.CANDIDATES);
        if (candidates.size() < 2) {
            trigger.next();
            return;
        }

        PrimaryStorageAllocationSpec spec = (PrimaryStorageAllocationSpec) data.get(PrimaryStorageConstant.AllocatorParams.SPEC);
        /* sort ps by availableVirtualCapacity in desc order */
        Comparator<PrimaryStorageVO> comparator = (o1, o2) -> {
            if (o1.getCapacity().getAvailableCapacity() > o2.getCapacity().getAvailableCapacity()) {
                return -1;
            } else {
                return 1;
            }
        };

        if (spec.getImageUuid() == null) {
            candidates.sort(comparator);
            trigger.next();
            return;
        }

        List<PrimaryStorageVO> ret = new ArrayList<>();

        Iterator<PrimaryStorageVO> it = candidates.iterator();
        PrimaryStorageVO next = it.next();

        String psType = next.getType();
        List<PrimaryStorageVO> tmp = new ArrayList<>();
        tmp.add(next);
        while (it.hasNext()) {
            next = it.next();
            if (next.getType().equals(psType)) {
                tmp.add(next);
            } else {
                tmp.sort(comparator);
                ret.addAll(tmp);

                psType = next.getType();
                tmp = new ArrayList<>();
                tmp.add(next);
            }
        }

        tmp.sort(comparator);
        ret.addAll(tmp);

        data.put(PrimaryStorageConstant.AllocatorParams.CANDIDATES, ret);

        trigger.next();
    }
}

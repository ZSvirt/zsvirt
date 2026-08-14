package org.zstack.storage.primary;


import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

import java.util.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageSortByVolumeQuantityFlow extends NoRollbackFlow {
    @Override
    public void run(FlowTrigger trigger, Map data) {
        List<PrimaryStorageVO> candidates = (List<PrimaryStorageVO>) data.get(PrimaryStorageConstant.AllocatorParams.CANDIDATES);
        if (candidates.size() < 2) {
            trigger.next();
            return;
        }

        PrimaryStorageAllocationSpec spec = (PrimaryStorageAllocationSpec) data.get(PrimaryStorageConstant.AllocatorParams.SPEC);
        /* sort ps by volume quantity in asc order */
        Comparator<PrimaryStorageVO> comparator = (ps1, ps2) -> {
            Long volumeQuantityInPs1 = Q.New(VolumeVO.class).eq(VolumeVO_.primaryStorageUuid, ps1.getUuid()).count();
            Long volumeQuantityInPs2 = Q.New(VolumeVO.class).eq(VolumeVO_.primaryStorageUuid, ps2.getUuid()).count();
            if (volumeQuantityInPs1 > volumeQuantityInPs2) {
                return 1;
            } else {
                return -1;
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

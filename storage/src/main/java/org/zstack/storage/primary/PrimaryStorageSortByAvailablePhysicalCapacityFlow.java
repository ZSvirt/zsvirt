package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.storage.backup.PrimaryStoragePriorityGetter;
import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by shixin.ruan on 2019/08/09.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageSortByAvailablePhysicalCapacityFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(PrimaryStorageSortByAvailablePhysicalCapacityFlow.class);


    @Override
    public void run(FlowTrigger trigger, Map data) {
        List<PrimaryStorageVO> candidates = (List<PrimaryStorageVO>) data.get(PrimaryStorageConstant.AllocatorParams.CANDIDATES);
        if (candidates.size() < 2) {
            trigger.next();
            return;
        }

        /* we assume that, before this flow, candidate has been sort by priority like this:
        * ShareBlock1, ShareBlock2, Ceph
        * after this flow, ShareBlock1, ShareBlock2 will be sorted by availablePhysicalCapacity */
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
                /* sort ps by availablePhysicalCapacity in desc order */
                Collections.sort(tmp, new Comparator<PrimaryStorageVO>() {
                    @Override
                    public int compare(PrimaryStorageVO o1, PrimaryStorageVO o2) {
                        if (o1.getCapacity().getAvailablePhysicalCapacity() > o2.getCapacity().getAvailablePhysicalCapacity()) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                });
                ret.addAll(tmp);

                psType = next.getType();
                tmp = new ArrayList<>();
                tmp.add(next);
            }
        }

        Collections.sort(tmp, new Comparator<PrimaryStorageVO>() {
            @Override
            public int compare(PrimaryStorageVO o1, PrimaryStorageVO o2) {
                if (o1.getCapacity().getAvailablePhysicalCapacity() > o2.getCapacity().getAvailablePhysicalCapacity()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        ret.addAll(tmp);

        data.put(PrimaryStorageConstant.AllocatorParams.CANDIDATES, ret);

        trigger.next();
    }
}

package org.zstack.storage.primary;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vo.ResourceVO;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageSortByCustomOrderFlow extends NoRollbackFlow {
    @Autowired
    DatabaseFacade dbf;
    @Override
    public void run(FlowTrigger trigger, Map data) {
        List<PrimaryStorageVO> candidates = (List<PrimaryStorageVO>) data.get(PrimaryStorageConstant.AllocatorParams.CANDIDATES);
        if (candidates.size() < 2) {
            trigger.next();
            return;
        }
        PrimaryStorageAllocationSpec spec = (PrimaryStorageAllocationSpec) data.get(PrimaryStorageConstant.AllocatorParams.SPEC);
        if (CollectionUtils.isEmpty(spec.getCandidatePrimaryStorageUuids())) {
            data.put(PrimaryStorageConstant.AllocatorParams.CANDIDATES, candidates);
            trigger.next();
            return;
        }
        Set<String> candidateUuids = candidates.stream().map(ResourceVO::getUuid).collect(Collectors.toSet());
        List<PrimaryStorageVO> ret = spec.getCandidatePrimaryStorageUuids().stream().filter(candidateUuids::contains)
                .map(psUuid -> dbf.findByUuid(psUuid, PrimaryStorageVO.class))
                .collect(Collectors.toList());

        data.put(PrimaryStorageConstant.AllocatorParams.CANDIDATES, ret);

        trigger.next();
    }
}

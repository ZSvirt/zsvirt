package org.zstack.allocator.domain.compute.flows;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.allocator.domain.compute.evaluator.AllocatorDomainPrimaryStorageGate;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;
import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.i18m;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DomainPrimaryStorageAllocatorFlow extends AbstractPrimaryStorageAllocatorFlow {
    private static final CLogger logger = Utils.getLogger(DomainPrimaryStorageAllocatorFlow.class);

    @Autowired(required = false)
    private AllocatorDomainPrimaryStorageGate gate;

    @Override
    public void allocate() {
        if (gate == null) {
            logger.warn("skip DomainPrimaryStorageAllocatorFlow: AllocatorDomainPrimaryStorageGate bean is not loaded");
            next();
            return;
        }

        List<PrimaryStorageVO> working = new ArrayList<>();
        for (PrimaryStorageCandidate candidate : candidates) {
            working.add(candidate.primaryStorage);
        }
        ErrorCode error = gate.filterRootVolume(spec, working);
        if (error != null) {
            rejectAll(error);
            next();
            return;
        }

        Set<String> allowed = new HashSet<>();
        for (PrimaryStorageVO vo : working) {
            if (vo != null && vo.getUuid() != null) {
                allowed.add(vo.getUuid());
            }
        }
        for (PrimaryStorageCandidate candidate : candidates) {
            if (!allowed.contains(candidate.getUuid())) {
                reject(candidate, i18m("not in allocator domain"));
            }
        }
        next();
    }
}

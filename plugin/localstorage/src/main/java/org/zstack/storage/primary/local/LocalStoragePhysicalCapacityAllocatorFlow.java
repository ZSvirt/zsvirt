package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;
import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;
import org.zstack.storage.primary.PrimaryStoragePhysicalCapacityManager;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalStoragePhysicalCapacityAllocatorFlow extends AbstractPrimaryStorageAllocatorFlow {
    @Autowired
    private PrimaryStoragePhysicalCapacityManager physicalCapacityMgr;

    @Override
    public void allocate() {
        for (PrimaryStorageCandidate candidate : candidates) {
            List<LocalStorageHostRefVO> kept = new ArrayList<>();
            ErrorCodeList errors = new ErrorCodeList();
            for (LocalStorageHostRefVO ref : LocalStorageCandidateRefs.get(candidate)) {
                if (physicalCapacityMgr.checkCapacityByRatio(ref.getPrimaryStorageUuid(), ref.getTotalPhysicalCapacity(), ref.getAvailablePhysicalCapacity())
                        && physicalCapacityMgr.checkRequiredCapacityByRatio(ref.getPrimaryStorageUuid(), ref.getTotalPhysicalCapacity(), spec.getTotalSize())) {
                    kept.add(ref);
                } else {
                    errors.add(operr("{the physical capacity usage of the host[uuid:%s] has exceeded the threshold[%s]}",
                            ref.getHostUuid(), physicalCapacityMgr.getRatio(ref.getPrimaryStorageUuid())));
                }
            }
            if (kept.isEmpty()) {
                reject(candidate, operr("failed allocate localstorage").withCause(errors));
            } else {
                LocalStorageCandidateRefs.set(candidate, kept);
            }
        }
        next();
    }
}

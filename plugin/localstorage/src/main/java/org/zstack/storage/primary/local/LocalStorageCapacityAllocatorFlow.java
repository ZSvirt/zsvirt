package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;
import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.utils.SizeUtils;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.i18m;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalStorageCapacityAllocatorFlow extends AbstractPrimaryStorageAllocatorFlow {
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;

    @Override
    public void allocate() {
        long reservedCapacity = SizeUtils.sizeStringToBytes(PrimaryStorageGlobalConfig.RESERVED_CAPACITY.value());
        for (PrimaryStorageCandidate candidate : candidates) {
            List<LocalStorageHostRefVO> kept = new ArrayList<>();
            for (LocalStorageHostRefVO ref : LocalStorageCandidateRefs.get(candidate)) {
                if (enough(ref, reservedCapacity)) {
                    kept.add(ref);
                }
            }
            if (kept.isEmpty()) {
                reject(candidate, i18m("no host on primary storage[uuid:%s] has available capacity greater than size[%s] after subtracting reserved capacity[%s]",
                        candidate.getUuid(), spec.getSize(), reservedCapacity));
            } else {
                LocalStorageCandidateRefs.set(candidate, kept);
            }
        }
        next();
    }

    private boolean enough(LocalStorageHostRefVO ref, long reservedCapacity) {
        if (spec.isNoOverProvisioning()) {
            return ref.getAvailableCapacity() - reservedCapacity >= spec.getSize();
        }
        return ratioMgr.calculatePrimaryStorageAvailableCapacityByRatio(ref.getPrimaryStorageUuid(),
                ref.getAvailableCapacity()) - reservedCapacity >= spec.getSize();
    }
}

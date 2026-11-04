package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.storage.primary.PrimaryStorageAllocationPurpose;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;
import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.i18m;
import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalStorageImageCacheAllocatorFlow extends AbstractPrimaryStorageAllocatorFlow {
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;

    @Override
    public void allocate() {
        if (!PrimaryStorageAllocationPurpose.CreateNewVm.toString().equals(spec.getPurpose())
                || spec.getImageUuid() == null) {
            next();
            return;
        }

        Long imageActualSize = Q.New(ImageVO.class)
                .eq(ImageVO_.uuid, spec.getImageUuid())
                .select(ImageVO_.actualSize)
                .findValue();
        if (imageActualSize == null) {
            fail(operr("no way to get image size of %s, report exception.", spec.getImageUuid()));
        }

        for (PrimaryStorageCandidate candidate : candidates) {
            List<LocalStorageHostRefVO> kept = new ArrayList<>();
            for (LocalStorageHostRefVO ref : LocalStorageCandidateRefs.get(candidate)) {
                if (hasImageCache(ref) || fitsImage(ref, imageActualSize)) {
                    kept.add(ref);
                }
            }
            if (kept.isEmpty()) {
                reject(candidate, i18m("no host on primary storage[uuid:%s] has image cache or capacity for image[uuid:%s]",
                        candidate.getUuid(), spec.getImageUuid()));
            } else {
                LocalStorageCandidateRefs.set(candidate, kept);
            }
        }
        next();
    }

    private boolean hasImageCache(LocalStorageHostRefVO ref) {
        Long count = Q.New(ImageCacheVO.class)
                .eq(ImageCacheVO_.primaryStorageUuid, ref.getPrimaryStorageUuid())
                .like(ImageCacheVO_.installUrl, String.format("%%hostUuid://%s%%", ref.getHostUuid()))
                .count();
        return count != null && count > 0;
    }

    private boolean fitsImage(LocalStorageHostRefVO ref, long imageActualSize) {
        if (spec.isNoOverProvisioning()) {
            return ref.getAvailableCapacity() > spec.getSize() + imageActualSize;
        }
        return ref.getAvailableCapacity() > ratioMgr.calculateByRatio(ref.getPrimaryStorageUuid(), spec.getSize()) + imageActualSize;
    }
}

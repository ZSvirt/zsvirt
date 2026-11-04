package org.zstack.storage.primary.allocate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.storage.primary.ImageCacheShadowVO;
import org.zstack.header.storage.primary.ImageCacheShadowVO_;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.storage.primary.PrimaryStorageAllocationPurpose;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;
import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.i18m;
import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageCapacityAllocatorFlow extends AbstractPrimaryStorageAllocatorFlow {
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;

    @Override
    public void allocate() {
        if (PrimaryStorageAllocationPurpose.CreateNewVm.toString().equals(spec.getPurpose())) {
            allocateForNewVm();
        } else {
            allocateByAvailableCapacity();
        }
        next();
    }

    private void allocateForNewVm() {
        if (spec.getImageUuid() == null) {
            return;
        }

        List<String> psUuids = allPrimaryStorageUuidList();
        Set<String> cached = new HashSet<>(Q.New(ImageCacheVO.class)
                .in(ImageCacheVO_.primaryStorageUuid, psUuids)
                .eq(ImageCacheVO_.imageUuid, spec.getImageUuid())
                .select(ImageCacheVO_.primaryStorageUuid)
                .listValues());
        long imageSize = imageSize(spec.getImageUuid());

        for (PrimaryStorageCandidate candidate : candidates) {
            PrimaryStorageVO vo = candidate.primaryStorage;
            boolean hit = cached.contains(candidate.getUuid());
            long requiredSize = ratioMgr.calculateByRatio(candidate.getUuid(), spec.getSize());
            if (!hit) {
                requiredSize += imageSize;
            }
            candidate.withOpaque(PrimaryStorageCandidate.OPAQUE_IMAGE_CACHE_HIT, hit);
            candidate.withOpaque(PrimaryStorageCandidate.OPAQUE_REQUIRED_SIZE, requiredSize);
            if (vo.getCapacity().getAvailableCapacity() < requiredSize) {
                reject(candidate, i18m("available capacity %s is less than required size %s",
                        vo.getCapacity().getAvailableCapacity(), requiredSize));
            }
        }
    }

    private void allocateByAvailableCapacity() {
        for (PrimaryStorageCandidate candidate : candidates) {
            PrimaryStorageVO vo = candidate.primaryStorage;
            long available = ratioMgr.calculatePrimaryStorageAvailableCapacityByRatio(
                    candidate.getUuid(), vo.getCapacity().getAvailableCapacity());
            candidate.withOpaque(PrimaryStorageCandidate.OPAQUE_REQUIRED_SIZE, spec.getSize());
            if (available <= spec.getSize()) {
                reject(candidate, i18m("available capacity %s is not greater than required size %s",
                        available, spec.getSize()));
            }
        }
    }

    private long imageSize(String imageUuid) {
        Long imageSize = Q.New(ImageVO.class)
                .eq(ImageVO_.uuid, imageUuid)
                .select(ImageVO_.actualSize)
                .findValue();
        if (imageSize == null) {
            imageSize = Q.New(ImageCacheVO.class)
                    .select(ImageCacheVO_.size)
                    .eq(ImageCacheVO_.imageUuid, imageUuid)
                    .limit(1)
                    .findValue();
        }
        if (imageSize == null) {
            imageSize = Q.New(ImageCacheShadowVO.class)
                    .select(ImageCacheShadowVO_.size)
                    .eq(ImageCacheShadowVO_.imageUuid, imageUuid)
                    .limit(1)
                    .findValue();
        }
        if (imageSize == null) {
            fail(operr("no way to get image size of %s, report exception.", imageUuid));
        }
        return imageSize;
    }
}

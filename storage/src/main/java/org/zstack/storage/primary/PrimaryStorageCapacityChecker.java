package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO_;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.utils.SizeUtils;

/**
 * Created by MaJin on 2021/7/21.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageCapacityChecker {
    @Autowired
    protected PrimaryStorageOverProvisioningManager psRatioMgr;
    @Autowired
    protected PrimaryStoragePhysicalCapacityManager physicalCapacityMgr;

    private String primaryStorageUuid;
    private long reservedCapacity;
    private long availableCapacity;
    private long totalPhysicalCapacity;
    private long availablePhysicalCapacity;

    public static PrimaryStorageCapacityChecker New(String primaryStorageUuid) {
        PrimaryStorageCapacityChecker checker = new PrimaryStorageCapacityChecker();
        checker.primaryStorageUuid = primaryStorageUuid;
        PrimaryStorageCapacityVO capacity = Q.New(PrimaryStorageCapacityVO.class).eq(PrimaryStorageCapacityVO_.uuid, primaryStorageUuid).find();
        checker.availableCapacity = capacity.getAvailableCapacity();
        checker.availablePhysicalCapacity = capacity.getAvailablePhysicalCapacity();
        checker.totalPhysicalCapacity = capacity.getTotalPhysicalCapacity();
        checker.reservedCapacity = SizeUtils.sizeStringToBytes(PrimaryStorageGlobalConfig.RESERVED_CAPACITY.value());
        return checker;
    }

    public static PrimaryStorageCapacityChecker New(PrimaryStorageCapacityVO capacity) {
        PrimaryStorageCapacityChecker checker = new PrimaryStorageCapacityChecker();
        checker.primaryStorageUuid = capacity.getUuid();
        checker.availableCapacity = capacity.getAvailableCapacity();
        checker.availablePhysicalCapacity = capacity.getAvailablePhysicalCapacity();
        checker.totalPhysicalCapacity = capacity.getTotalPhysicalCapacity();
        checker.reservedCapacity = SizeUtils.sizeStringToBytes(PrimaryStorageGlobalConfig.RESERVED_CAPACITY.value());
        return checker;
    }

    public static PrimaryStorageCapacityChecker New(String primaryStorageUuid, long availableCapacity, long totalPhysicalCapacity, long availablePhysicalCapacity) {
        PrimaryStorageCapacityChecker checker = new PrimaryStorageCapacityChecker();
        checker.primaryStorageUuid = primaryStorageUuid;
        checker.availableCapacity = availableCapacity;
        checker.availablePhysicalCapacity = availablePhysicalCapacity;
        checker.totalPhysicalCapacity = totalPhysicalCapacity;
        checker.reservedCapacity = SizeUtils.sizeStringToBytes(PrimaryStorageGlobalConfig.RESERVED_CAPACITY.value());
        return checker;
    }

    public boolean checkRequiredSize(long requiredSize) {
        return checkIncreasedAndTotalRequiredSize(requiredSize, requiredSize);
    }

    public boolean checkIncreasedAndTotalRequiredSize(long increasedRequiredSize, long totalRequiredSize) {
        boolean availableCapacityMeetIncreaseSizeByRatio = availableCapacity
                - psRatioMgr.calculateByRatio(primaryStorageUuid, increasedRequiredSize) >= reservedCapacity;
        boolean physicalCapacityHasFreeSpaceByRatio = physicalCapacityMgr
                .checkCapacityByRatio(primaryStorageUuid, totalPhysicalCapacity, availablePhysicalCapacity);
        boolean physicalCapacityMeetTotalRequiredSizeByRatio = physicalCapacityMgr
                .checkRequiredCapacityByRatio(primaryStorageUuid, totalPhysicalCapacity, totalRequiredSize);

        return availableCapacityMeetIncreaseSizeByRatio &&
                physicalCapacityHasFreeSpaceByRatio &&
                physicalCapacityMeetTotalRequiredSizeByRatio;
    }
}

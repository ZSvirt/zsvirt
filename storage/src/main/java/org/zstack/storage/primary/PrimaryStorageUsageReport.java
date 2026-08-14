package org.zstack.storage.primary;

import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageHistoricalUsageVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class PrimaryStorageUsageReport extends
        AbstractUsageReport<PrimaryStorageHistoricalUsageVO, PrimaryStorageCapacityVO> implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(PrimaryStorageUsageReport.class);

    {
        usageClass = PrimaryStorageHistoricalUsageVO.class;
        capacityClass = PrimaryStorageCapacityVO.class;
    }

    @Override
    public void managementNodeReady() {
        start();
    }
}

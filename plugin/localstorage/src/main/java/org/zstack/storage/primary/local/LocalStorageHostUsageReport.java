package org.zstack.storage.primary.local;

import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.storage.primary.AbstractUsageReport;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class LocalStorageHostUsageReport extends
        AbstractUsageReport<LocalStorageHostHistoricalUsageVO, LocalStorageHostRefVO> implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(LocalStorageHostUsageReport.class);

    {
        capacityClass = LocalStorageHostRefVO.class;
        usageClass = LocalStorageHostHistoricalUsageVO.class;
    }

    @Override
    public void managementNodeReady() {
        // UI only show localStoragePrimaryStorage Capacity Usage, don't show the localHost Capacity Usage.
        // therefore, do not need to collect and forecast localHost Capacity Usage.
        //start();
    }
}

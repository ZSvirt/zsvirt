package org.zstack.storage.ceph.primary;

import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.storage.primary.AbstractUsageReport;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class PoolUsageReport extends
        AbstractUsageReport<CephOsdGroupHistoricalUsageVO, CephOsdGroupVO> implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(PoolUsageReport.class);

    {
        capacityClass = CephOsdGroupVO.class;
        usageClass = CephOsdGroupHistoricalUsageVO.class;
    }

    @Override
    public void managementNodeReady() {
        start();
    }
}

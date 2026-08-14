package org.zstack.drs;

import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.mevoco.PremiumGlobalConfig;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 * Created by lining on 2019/12/12.
 */
@GlobalConfigDefinition
public class DRSGlobalConfig {
    public static final String CATEGORY = "drs";

    @GlobalConfigValidation(min = 300, max = 168 * 3600)
    @BindResourceConfig(value = {ClusterVO.class})
    public static PremiumGlobalConfig DRS_SCHEDULING_INTERVAL = new PremiumGlobalConfig(CATEGORY, "drs.schedulingInterval");

    @GlobalConfigValidation(min = 0, max = 100)
    @BindResourceConfig(value = {ClusterVO.class})
    public static PremiumGlobalConfig DRS_MIGRATE_VM_CONCURRENT = new PremiumGlobalConfig(CATEGORY, "drs.migrateVm.concurrent");

    // seconds
    @GlobalConfigValidation(min = 0, max = 6 * 3600)
    public static PremiumGlobalConfig DRS_COLLECT_HOST_METRIC_DATA_DURATION = new PremiumGlobalConfig(CATEGORY, "drs.collectHostMetricDataDuration");
}


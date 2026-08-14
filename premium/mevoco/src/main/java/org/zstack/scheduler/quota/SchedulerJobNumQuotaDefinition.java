package org.zstack.scheduler.quota;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.scheduler.SchedulerJobVO;
import org.zstack.identity.ResourceHelper;
import org.zstack.scheduler.SchedulerQuotaConstant;
import org.zstack.scheduler.SchedulerQuotaGlobalConfig;

public class SchedulerJobNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return SchedulerQuotaConstant.SCHEDULER_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return SchedulerQuotaGlobalConfig.SCHEDULER_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return ResourceHelper.countOwnResources(SchedulerJobVO.class, accountUuid);
    }
}

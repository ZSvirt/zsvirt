package org.zstack.scheduler.quota;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.scheduler.SchedulerTriggerVO;
import org.zstack.identity.ResourceHelper;
import org.zstack.scheduler.SchedulerQuotaConstant;
import org.zstack.scheduler.SchedulerQuotaGlobalConfig;

public class SchedulerTriggerNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return SchedulerQuotaConstant.SCHEDULER_TRIGGER_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return SchedulerQuotaGlobalConfig.SCHEDULER_TRIGGER_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return ResourceHelper.countOwnResources(SchedulerTriggerVO.class, accountUuid);
    }
}

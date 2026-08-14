package org.zstack.zwatch.quota;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.identity.ResourceHelper;
import org.zstack.zwatch.ZWatchQuotaConstant;
import org.zstack.zwatch.ZWatchQuotaGlobalConfig;
import org.zstack.zwatch.alarm.EventSubscriptionVO;

public class ZWatchEventNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return ZWatchQuotaConstant.ZWATCH_EVENT_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return ZWatchQuotaGlobalConfig.ZWATCH_EVENT_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return ResourceHelper.countOwnResources(EventSubscriptionVO.class, accountUuid);
    }
}

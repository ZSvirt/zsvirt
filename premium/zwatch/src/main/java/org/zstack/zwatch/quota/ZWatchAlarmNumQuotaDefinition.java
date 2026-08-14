package org.zstack.zwatch.quota;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.identity.ResourceHelper;
import org.zstack.zwatch.ZWatchQuotaConstant;
import org.zstack.zwatch.ZWatchQuotaGlobalConfig;
import org.zstack.zwatch.alarm.AlarmVO;

public class ZWatchAlarmNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return ZWatchQuotaConstant.ZWATCH_ALARM_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return ZWatchQuotaGlobalConfig.ZWATCH_ALARM_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return ResourceHelper.countOwnResources(AlarmVO.class, accountUuid);
    }
}

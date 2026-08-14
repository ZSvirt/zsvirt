package org.zstack.network.service.vip;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.identity.ResourceHelper;

public class VipNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return VipQuotaConstant.VIP_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return VipQuotaGlobalConfig.VIP_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return ResourceHelper.countOwnResources(VipVO.class, accountUuid);
    }
}

package org.zstack.network.service.eip;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.identity.ResourceHelper;

public class EipNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return EipQuotaConstant.EIP_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return EipQuotaGlobalConfig.EIP_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return ResourceHelper.countOwnResources(EipVO.class, accountUuid);
    }
}

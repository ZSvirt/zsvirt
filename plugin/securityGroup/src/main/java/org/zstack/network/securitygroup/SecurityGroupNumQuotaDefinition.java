package org.zstack.network.securitygroup;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.identity.ResourceHelper;

public class SecurityGroupNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return SecurityGroupQuotaConstant.SG_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return SecurityGroupQuotaGlobalConfig.SG_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return ResourceHelper.countOwnResources(SecurityGroupVO.class, accountUuid);
    }
}

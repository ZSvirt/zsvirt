package org.zstack.network.service.portforwarding;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.identity.ResourceHelper;

public class PortForwardingNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return PortForwardingQuotaConstant.PF_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return PortFowardingQuotaGlobalConfig.PF_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return ResourceHelper.countOwnResources(PortForwardingRuleVO.class, accountUuid);
    }
}

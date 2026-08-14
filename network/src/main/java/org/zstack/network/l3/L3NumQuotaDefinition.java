package org.zstack.network.l3;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.network.l3.L3NetworkQuotaConstant;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.identity.ResourceHelper;

public class L3NumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return L3NetworkQuotaConstant.L3_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return L3NetworkQuotaGlobalConfig.L3_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return ResourceHelper.countOwnResources(L3NetworkVO.class, accountUuid);
    }
}

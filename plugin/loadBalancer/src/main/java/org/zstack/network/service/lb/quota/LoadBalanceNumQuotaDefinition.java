package org.zstack.network.service.lb.quota;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.identity.ResourceHelper;
import org.zstack.network.service.lb.LoadBalanceQuotaConstant;
import org.zstack.network.service.lb.LoadBalanceQuotaGlobalConfig;
import org.zstack.network.service.lb.LoadBalancerVO;

public class LoadBalanceNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return LoadBalanceQuotaConstant.LOAD_BALANCER_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return LoadBalanceQuotaGlobalConfig.LOAD_BALANCER_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return ResourceHelper.countOwnResources(LoadBalancerVO.class, accountUuid);
    }
}

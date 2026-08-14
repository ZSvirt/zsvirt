package org.zstack.sns;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.identity.ResourceHelper;

public class SNSEndpointNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return SNSQuotaConstant.SNS_ENDPOINT_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return SNSQuotaGlobalConfig.SNS_ENDPOINT_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return ResourceHelper.countOwnResources(SNSApplicationEndpointVO.class, accountUuid);
    }
}

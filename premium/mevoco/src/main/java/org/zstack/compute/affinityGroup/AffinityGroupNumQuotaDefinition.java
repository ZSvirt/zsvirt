package org.zstack.compute.affinityGroup;

import org.zstack.header.affinitygroup.AffinityGroupConstants;
import org.zstack.header.affinitygroup.AffinityGroupVO;
import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.identity.ResourceHelper;

public class AffinityGroupNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return AffinityGroupConstants.AFFINITYGROUP_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return AffinityGroupGlobalConfig.AFFINITYGROUP_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return ResourceHelper.countOwnResources(AffinityGroupVO.class, accountUuid);
    }
}

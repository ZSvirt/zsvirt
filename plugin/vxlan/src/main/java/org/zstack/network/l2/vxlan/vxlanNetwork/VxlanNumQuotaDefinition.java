package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.identity.ResourceHelper;

public class VxlanNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return VxlanNetworkQuotaConstant.VXLAN_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return VxlanNetworkQuotaGlobalConfig.VXLAN_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return ResourceHelper.countOwnResources(VxlanNetworkVO.class, accountUuid);
    }
}

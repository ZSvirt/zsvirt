package org.zstack.pciDevice.quota;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.pciDevice.PciDeviceConstants;
import org.zstack.pciDevice.PciDeviceQuotaGlobalConfig;
import org.zstack.pciDevice.PciDeviceQuotaUtils;

public class GPUNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return PciDeviceConstants.GPU_NUMBER;
    }

    @Override
    public Long getDefaultValue() {
        return PciDeviceQuotaGlobalConfig.GPU_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return PciDeviceQuotaUtils.getUsedPci(accountUuid).get(PciDeviceConstants.GPU_Video_Controller);
    }
}

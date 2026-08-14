package org.zstack.pciDevice.quota;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.pciDevice.PciDeviceConstants;
import org.zstack.pciDevice.PciDeviceQuotaGlobalConfig;
import org.zstack.pciDevice.PciDeviceQuotaUtils;

public class GenericPCIDeviceNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return PciDeviceConstants.GENERIC_PCI_DEVICE_NUMBER;
    }

    @Override
    public Long getDefaultValue() {
        return PciDeviceQuotaGlobalConfig.PCI_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return PciDeviceQuotaUtils.getUsedPci(accountUuid).get(PciDeviceConstants.Generic);
    }
}

package org.zstack.pciDevice;

import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.core.config.GlobalConfig;
import org.zstack.identity.QuotaGlobalConfig;

@GlobalConfigDefinition
public class PciDeviceQuotaGlobalConfig extends QuotaGlobalConfig {

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig PCI_NUM = new GlobalConfig(CATEGORY, PciDeviceConstants.GENERIC_PCI_DEVICE_NUMBER);

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig GPU_NUM = new GlobalConfig(CATEGORY, PciDeviceConstants.GPU_NUMBER);
}

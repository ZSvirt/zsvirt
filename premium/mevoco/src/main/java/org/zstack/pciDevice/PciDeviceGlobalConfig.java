package org.zstack.pciDevice;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.resourceconfig.BindResourceConfig;

@GlobalConfigDefinition
public class PciDeviceGlobalConfig {
    public static final String CATEGORY = "pciDevice";

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig ENABLE_IOMMU = new GlobalConfig(CATEGORY, "iommuEnabled");

    @GlobalConfigValidation(validValues = {"true", "false"})
    @BindResourceConfig(value = {VmInstanceVO.class, HostVO.class, ClusterVO.class})
    public static GlobalConfig ENABLE_HOT_PLUG = new GlobalConfig(CATEGORY, "hotPlugEnabled");
}
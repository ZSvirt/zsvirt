package org.zstack.pciDevice;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostIommuGetter {
    private static final CLogger logger = Utils.getLogger(HostIommuGetter.class);

    @Autowired
    private PluginRegistry pluginRgty;

    public HostIommuStateType getState(String hostUuid) {
        String state = PciDeviceSystemTags.HOST_IOMMU_STATE.getTokenByResourceUuid(hostUuid, PciDeviceSystemTags.HOST_IOMMU_STATE_TOKEN);
        if (state == null) {
            return HostIommuStateType.Disabled;
        }
        return HostIommuStateType.valueOf(state);
    }

    public HostIommuStatusType getStatus(String hostUuid) {
        String state = PciDeviceSystemTags.HOST_IOMMU_STATUS.getTokenByResourceUuid(hostUuid, PciDeviceSystemTags.HOST_IOMMU_STATUS_TOKEN);
        if (state == null) {
            return HostIommuStatusType.Inactive;
        }
        return HostIommuStatusType.valueOf(state);
    }
}

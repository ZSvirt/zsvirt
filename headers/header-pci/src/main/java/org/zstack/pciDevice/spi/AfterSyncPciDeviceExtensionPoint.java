package org.zstack.pciDevice.spi;

import org.zstack.header.core.Completion;
import org.zstack.pciDevice.message.PciDeviceTO;

import java.util.List;

public interface AfterSyncPciDeviceExtensionPoint {
    void afterSyncPciDeviceVO(String hostUuid, List<PciDeviceTO> tos);
}

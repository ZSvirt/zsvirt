package org.zstack.header.host;

import org.zstack.header.core.Completion;
import org.zstack.pciDevice.PciDeviceTO;

import java.util.List;

public interface AfterSyncPciDeviceExtensionPoint {
    void afterSyncPciDeviceVO(String hostUuid, List<PciDeviceTO> tos);
}

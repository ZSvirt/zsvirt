package org.zstack.header.storage.primary;

import java.util.List;

public interface HostBlockDeviceForPrimaryStorageExtensionPoint {
    List<String> getBlockDevicesForPrimaryStorage(String hostUuid);
}

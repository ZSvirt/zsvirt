package org.zstack.header.host;

import java.util.Collection;

public interface HostBlockDeviceUsageExtensionPoint {
    Collection<String> getUsedBlockDeviceIdentifiers();
}

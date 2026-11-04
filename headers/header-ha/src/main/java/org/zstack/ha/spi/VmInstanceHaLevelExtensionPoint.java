package org.zstack.ha.spi;

import org.zstack.ha.header.VmHaLevel;

public interface VmInstanceHaLevelExtensionPoint {
    void afterSetVmInstanceHaLevel(String vmInstanceUuid, VmHaLevel originLevel, VmHaLevel currentLevel);
}

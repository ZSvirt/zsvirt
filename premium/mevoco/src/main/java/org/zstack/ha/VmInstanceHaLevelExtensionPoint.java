package org.zstack.ha;

public interface VmInstanceHaLevelExtensionPoint {
    void afterSetVmInstanceHaLevel(String vmInstanceUuid, VmHaLevel originLevel, VmHaLevel currentLevel);
}

package org.zstack.header.volume;

import org.zstack.header.storage.primary.InstantiateVolumeOnPrimaryStorageMsg;

public interface AfterInstantiateVolumeExtensionPoint {
    void afterInstantiateVolume(InstantiateVolumeOnPrimaryStorageMsg msg);
}

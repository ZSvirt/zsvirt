package org.zstack.storage.volume;

import org.zstack.header.errorcode.ErrorableValue;

public interface VolumeManager {
    ErrorableValue<String> findConnectedKvmHostByPrimaryStorage(String primaryStorageUuid, String storageDescription, String operation);
}

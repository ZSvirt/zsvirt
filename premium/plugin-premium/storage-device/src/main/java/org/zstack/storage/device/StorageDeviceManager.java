package org.zstack.storage.device;

import org.zstack.header.Service;

/**
 * Create by weiwang at 2018/8/3
 */
public interface StorageDeviceManager extends Service {
    StorageDeviceBackend getStorageDeviceBackend(String hypervisorType);
}

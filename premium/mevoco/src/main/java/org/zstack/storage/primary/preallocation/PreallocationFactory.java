package org.zstack.storage.primary.preallocation;

/**
 * author:kaicai.hu
 * Date:2019/8/20
 */
public interface PreallocationFactory {
    String getPrimaryStorageType();

    String getPreallocation(String primaryStorageUuid);

    String getProvisioningStrategy(String primaryStorageUuid);
}

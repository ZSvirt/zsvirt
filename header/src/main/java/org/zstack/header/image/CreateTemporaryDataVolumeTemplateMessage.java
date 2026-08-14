package org.zstack.header.image;

import java.util.List;

/**
 * Created by MaJin on 2021/3/18.
 */
public interface CreateTemporaryDataVolumeTemplateMessage extends CreateDataVolumeTemplateMessage {
    @Override
    default void setName(String name) {}

    @Override
    default void setDescription(String description) {}

    @Override
    default String getDescription() {
        return null;
    }

    @Override
    default void setResourceUuid(String resourceUuid) {}

    @Override
    default String getResourceUuid() {
        return null;
    }

    @Override
    default void setBackupStorageUuids(List<String> backupStorageUuids) {}

    @Override
    default List<String> getBackupStorageUuids() {
        return null;
    }
}

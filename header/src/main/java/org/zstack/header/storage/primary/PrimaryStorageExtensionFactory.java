package org.zstack.header.storage.primary;

import java.util.List;

public interface PrimaryStorageExtensionFactory {
    String getPrimaryStorageType();

    PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo);

    List<Class> getMessageClasses();
}

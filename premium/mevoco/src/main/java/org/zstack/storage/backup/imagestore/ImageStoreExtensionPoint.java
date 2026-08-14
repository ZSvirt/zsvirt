package org.zstack.storage.backup.imagestore;

import org.zstack.header.core.Completion;

/**
 * Created by mingjian.deng on 2019/9/11.
 */
public interface ImageStoreExtensionPoint {
    void addMoreAgentInBackupStorage(final ImageStoreBackupStorageVO vo, Completion completion);
}

package org.zstack.header.imagestore;

import org.zstack.header.core.workflow.Flow;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageInventory;

import java.util.List;

/**
 * Created by MaJin on 2019/4/29.
 */
public interface ImageStorageContinueConnectExtensionPoint {
    List<Flow> createImageStoreConnectingFlow(boolean newAdded, ImageStoreBackupStorageInventory inv);
}

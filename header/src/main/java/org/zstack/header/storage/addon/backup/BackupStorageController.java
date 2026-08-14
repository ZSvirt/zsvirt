package org.zstack.header.storage.addon.backup;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.addon.ImageDescriptor;
import org.zstack.header.storage.addon.StorageCapacity;
import org.zstack.header.storage.addon.StorageHealthy;

public interface BackupStorageController {
    String getIdentity();
    void connect(boolean newAdded, String url, Completion comp);

    void reportCapacity(ReturnValueCompletion<StorageCapacity> comp);
    void reportHealthy(ReturnValueCompletion<StorageHealthy> comp);

    void importImage(ImageInventory image, ReturnValueCompletion<ImageDescriptor> comp);
    void cancelImport(ImageInventory image, Completion comp);
}

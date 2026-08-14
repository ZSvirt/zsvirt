package org.zstack.storage.addon.primary;

import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.storage.primary.ImageCacheCleaner;

public class ExternalPrimaryStorageImageCacheCleaner extends ImageCacheCleaner implements ManagementNodeReadyExtensionPoint {
    @Override
    public void managementNodeReady() {
        startGC();
    }

    @Override
    protected String getPrimaryStorageType() {
        return PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE;
    }

}

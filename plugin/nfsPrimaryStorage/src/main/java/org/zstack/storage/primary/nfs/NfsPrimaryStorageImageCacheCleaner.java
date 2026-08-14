package org.zstack.storage.primary.nfs;

import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.storage.primary.ImageCacheCleaner;

/**
 * Created by xing5 on 2016/7/20.
 */
public class NfsPrimaryStorageImageCacheCleaner extends ImageCacheCleaner implements ManagementNodeReadyExtensionPoint {
    @Override
    public void managementNodeReady() {
        startGC();
    }

    @Override
    protected String getPrimaryStorageType() {
        return NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE;
    }
}

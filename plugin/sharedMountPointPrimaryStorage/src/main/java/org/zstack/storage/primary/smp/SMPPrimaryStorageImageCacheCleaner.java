package org.zstack.storage.primary.smp;

import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.storage.primary.ImageCacheCleaner;

/**
 * Created by mingjian.deng on 2018/1/24.
 */
public class SMPPrimaryStorageImageCacheCleaner extends ImageCacheCleaner implements ManagementNodeReadyExtensionPoint {
    @Override
    public void managementNodeReady() {
        startGC();
    }

    @Override
    protected String getPrimaryStorageType() {
        return SMPConstants.SMP_TYPE;
    }
}
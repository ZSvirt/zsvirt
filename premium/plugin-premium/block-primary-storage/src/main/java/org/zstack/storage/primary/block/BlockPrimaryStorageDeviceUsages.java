package org.zstack.storage.primary.block;

import org.zstack.core.db.Q;
import org.zstack.header.host.HostBlockDeviceUsageExtensionPoint;

import java.util.Collection;

public class BlockPrimaryStorageDeviceUsages implements HostBlockDeviceUsageExtensionPoint {
    @Override
    public Collection<String> getUsedBlockDeviceIdentifiers() {
        // These records own volume, image-cache and in-flight LUNs until their cleanup completes.
        return Q.New(BlockScsiLunVO.class).select(BlockScsiLunVO_.wwn).listValues();
    }
}

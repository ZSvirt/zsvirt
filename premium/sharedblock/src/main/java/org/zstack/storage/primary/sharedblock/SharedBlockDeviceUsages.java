package org.zstack.storage.primary.sharedblock;

import org.zstack.core.db.Q;
import org.zstack.header.host.HostBlockDeviceUsageExtensionPoint;

import java.util.Collection;

public class SharedBlockDeviceUsages implements HostBlockDeviceUsageExtensionPoint {
    @Override
    public Collection<String> getUsedBlockDeviceIdentifiers() {
        return Q.New(SharedBlockVO.class).select(SharedBlockVO_.diskUuid).listValues();
    }
}

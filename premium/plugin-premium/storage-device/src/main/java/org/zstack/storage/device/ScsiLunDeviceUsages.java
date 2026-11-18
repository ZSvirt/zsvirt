package org.zstack.storage.device;

import org.zstack.core.db.Q;
import org.zstack.header.host.HostBlockDeviceUsageExtensionPoint;
import org.zstack.header.storageDevice.ScsiLunVO;
import org.zstack.header.storageDevice.ScsiLunVO_;
import org.zstack.header.storageDevice.ScsiLunVmInstanceRefVO;
import org.zstack.header.storageDevice.ScsiLunVmInstanceRefVO_;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScsiLunDeviceUsages implements HostBlockDeviceUsageExtensionPoint {
    @Override
    public Collection<String> getUsedBlockDeviceIdentifiers() {
        List<String> lunUuids = Q.New(ScsiLunVmInstanceRefVO.class)
                .select(ScsiLunVmInstanceRefVO_.scsiLunUuid).listValues();
        if (lunUuids.isEmpty()) {
            return Collections.emptyList();
        }
        return Q.New(ScsiLunVO.class).select(ScsiLunVO_.wwid, ScsiLunVO_.wwn)
                .in(ScsiLunVO_.uuid, lunUuids).listTuple().stream()
                .flatMap(tuple -> Stream.of(tuple.get(0, String.class), tuple.get(1, String.class)))
                .collect(Collectors.toList());
    }
}

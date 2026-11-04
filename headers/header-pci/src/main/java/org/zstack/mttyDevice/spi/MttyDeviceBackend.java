package org.zstack.mttyDevice.spi;

import org.zstack.header.core.Completion;
import org.zstack.header.host.HypervisorType;
import org.zstack.mttyDevice.entity.MttyDeviceInventory;

import java.util.List;

public interface MttyDeviceBackend {
    HypervisorType getHypervisorType();
    void generateSeMdevDevices(String hostUuid, MttyDeviceInventory mttyDevice, List<String> mdevUuids, Boolean reSplite, Completion completion);
    void ungenerateSeMdevDevices(String hostUuid, MttyDeviceInventory mttyDevice, List<String> mdevUuids, Completion completion);
    void syncMttyDeviceFromHost(String hostUuid, Completion completion);
}

package org.zstack.header.storage.migration;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.storage.primary.InstantiateTemporaryVolumeOnPrimaryStorageMsg;
import org.zstack.header.volume.CreateVolumeMsg;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface KvmStorageLiveMigrationExtensionPoint {
    default List<Flow> getAllocateTemporaryVolumeFlow(String vmUuid,
                                                      String srcHostUuid,
                                                      List<VolumeVO> volumesToMigrate,
                                                      Map<String, Long> sourceVolumeVirtualSizes) {
        return Collections.emptyList();
    }

    default ErrorCode preCreateTemporaryVolume(CreateVolumeMsg msg,
                                               VolumeVO sourceVolume,
                                               Map<String, Long> sourceVolumeVirtualSizes) {
        return null;
    }

    default ErrorCode afterCreateTemporaryVolume(VolumeVO sourceVolume, VolumeInventory temporaryVolume) {
        return null;
    }

    default ErrorCode beforeInstantiateTemporaryVolume(String hostUuid,
                                                       VolumeInventory temporaryVolume,
                                                       InstantiateTemporaryVolumeOnPrimaryStorageMsg msg) {
        return null;
    }
}

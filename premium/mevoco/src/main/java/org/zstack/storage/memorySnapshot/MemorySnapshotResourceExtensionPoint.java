package org.zstack.storage.memorySnapshot;

import org.zstack.header.core.Completion;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataVO;

import java.util.List;
import java.util.Map;

/**
 * Created by LiangHanYu on 2022/6/1 14:02
 */
public interface MemorySnapshotResourceExtensionPoint {
    String getArchiveBundleCanonicalName();

    Class<?> getArchiveBundleClass();

    void recoverDeviceByAddress(String vmInstanceUuid, String resourceUuid, List<?> bundles, Completion completion);

    void archiveDeviceAddressByResources(String vmInstanceUuid, Completion completion);

    void rollBackResourceAndConfigs(String vmInstanceUuid, Map<String, VmInstanceResourceMetadataVO> originDeviceAddressByResourceUuid, Completion completion);
}

package org.zstack.storage.memorySnapshot;

import org.zstack.header.vm.devices.VmInstanceResourceMetadataVO;

import java.util.List;

public interface ResourceMetadataBuilder {
    List<VmInstanceResourceMetadataVO> buildResourceMetadataVOs(String vmInstanceUuid);
}

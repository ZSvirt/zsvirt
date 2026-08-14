package org.zstack.network.service.eip;

import org.zstack.header.vm.VmInstanceSpec;

import java.util.List;
import java.util.Map;

public interface WorkOutEipStructureExtensionPoint {
    void afterWorkOutEipStruct(VmInstanceSpec spec, Map<String, List<EipStruct>> map);
}

package org.zstack.compute.sriov;

import org.zstack.header.core.Completion;
import org.zstack.header.host.HypervisorType;

import java.util.List;
import java.util.Map;

/**
 * Created by GuoYi on 4/13/20.
 */
public interface VmVfNicHypervisorBackend {
    HypervisorType getHypervisorType();

    void addBridgeFdbEntryForInnerNics(List<String> hostUuids, Completion completion);
    void addBridgeFdbEntryForVmNics(Map<String, List<String>> hostNicMacs, String physicalInterface, Completion completion);
}

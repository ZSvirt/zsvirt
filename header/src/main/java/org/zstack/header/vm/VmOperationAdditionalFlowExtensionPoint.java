package org.zstack.header.vm;

import org.zstack.header.core.workflow.Flow;

import java.util.List;

public interface VmOperationAdditionalFlowExtensionPoint {
    List<Flow> getBeforeFormalWorkFlows(VmInstanceSpec spec);

    List<Flow> getAdditionalVmOperationFlows(VmInstanceSpec spec);
}

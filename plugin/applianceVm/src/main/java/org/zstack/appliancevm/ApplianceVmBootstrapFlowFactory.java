package org.zstack.appliancevm;

import org.zstack.header.core.workflow.Flow;

/**
 */
public interface ApplianceVmBootstrapFlowFactory {
    String getHypervisorTypeForApplianceVmBootstrapFlow();

    Flow createApplianceVmBootstrapInfoFlow();
}

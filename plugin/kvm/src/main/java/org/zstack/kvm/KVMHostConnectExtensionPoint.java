package org.zstack.kvm;

import org.zstack.header.core.workflow.Flow;

public interface KVMHostConnectExtensionPoint {
    Flow createKvmHostConnectingFlow(KVMHostConnectedContext context);
}

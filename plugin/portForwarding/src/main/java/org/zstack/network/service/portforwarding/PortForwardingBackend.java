package org.zstack.network.service.portforwarding;

import org.zstack.header.core.Completion;
import org.zstack.header.network.service.NetworkServiceProviderType;

public interface PortForwardingBackend {
    NetworkServiceProviderType getProviderType();
    
    void applyPortForwardingRule(PortForwardingStruct struct, Completion completion);
    
    void revokePortForwardingRule(PortForwardingStruct struct, Completion completion);
}

package org.zstack.network.service.portforwarding;

import org.zstack.header.core.Completion;

public interface PortForwardingManager {
    PortForwardingBackend getPortForwardingBackend(String providerType);

    void attachPortForwardingRule(PortForwardingStruct struct, String providerType, Completion completion);

    void detachPortForwardingRule(PortForwardingStruct struct, String providerType, Completion completion);

    PortForwardingStruct makePortForwardingStruct(PortForwardingRuleInventory rule);
}

package org.zstack.network.service.portforwarding;

import org.zstack.header.network.service.NetworkServiceProviderType;

public interface AttachPortForwardingRuleExtensionPoint {
    void preAttachPortForwardingRule(PortForwardingRuleInventory inv, NetworkServiceProviderType serviceProviderType) throws PortForwardingException;
    
    void beforeAttachPortForwardingRule(PortForwardingRuleInventory inv, NetworkServiceProviderType serviceProviderType);
    
    void afterAttachPortForwardingRule(PortForwardingRuleInventory inv, NetworkServiceProviderType serviceProviderType);
    
    void failToAttachPortForwardingRule(PortForwardingRuleInventory inv, NetworkServiceProviderType serviceProviderType);
}

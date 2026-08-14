package org.zstack.network.service.portforwarding;

import org.zstack.header.network.service.NetworkServiceProviderType;

public interface RevokePortForwardingRuleExtensionPoint {
    void preRevokePortForwardingRule(PortForwardingRuleInventory inv, NetworkServiceProviderType serviceProviderType) throws PortForwardingException;
    
    void beforeRevokePortForwardingRule(PortForwardingRuleInventory inv, NetworkServiceProviderType serviceProviderType);
    
    void afterRevokePortForwardingRule(PortForwardingRuleInventory inv, NetworkServiceProviderType serviceProviderType);
    
    void failToRevokePortForwardingRule(PortForwardingRuleInventory inv, NetworkServiceProviderType serviceProviderType);
}

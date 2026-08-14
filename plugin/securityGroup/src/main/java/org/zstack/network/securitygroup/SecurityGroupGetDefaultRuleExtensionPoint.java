package org.zstack.network.securitygroup;

import java.util.List;

public interface SecurityGroupGetDefaultRuleExtensionPoint {
    List<String> getGroupMembers(String sgUuid, int ipVersion);
}

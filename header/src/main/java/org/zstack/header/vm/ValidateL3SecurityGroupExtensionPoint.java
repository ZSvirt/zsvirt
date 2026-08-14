package org.zstack.header.vm;

import java.util.List;

public interface ValidateL3SecurityGroupExtensionPoint {
    void validateSystemtagL3SecurityGroup(String l3Uuid, List<String> securityGroupUuids);
}

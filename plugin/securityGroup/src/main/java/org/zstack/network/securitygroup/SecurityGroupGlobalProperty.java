package org.zstack.network.securitygroup;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class SecurityGroupGlobalProperty {
    @GlobalProperty(name="upgradeSecurityGroup", defaultValue = "false")
    public static boolean UPGRADE_SECURITY_GROUP;
}

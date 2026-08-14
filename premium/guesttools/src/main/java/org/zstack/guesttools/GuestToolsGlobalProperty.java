package org.zstack.guesttools;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class GuestToolsGlobalProperty {
    @GlobalProperty(name="upgradeLinuxGuestTools", defaultValue = "false")
    public static boolean UPGRADE_LINUX_GUESTTOOLS;
}

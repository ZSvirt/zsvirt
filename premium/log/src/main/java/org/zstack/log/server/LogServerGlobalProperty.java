package org.zstack.log.server;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class LogServerGlobalProperty {
    @GlobalProperty(name="upgradeLogLabelToLogServer", defaultValue = "false")
    public static boolean UPGRADE_LOG_LABEL_TO_LOG_SERVER;
}

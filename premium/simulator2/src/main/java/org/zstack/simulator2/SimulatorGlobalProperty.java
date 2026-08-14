package org.zstack.simulator2;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by xing5 on 2017/9/16.
 */
@GlobalPropertyDefinition
public class SimulatorGlobalProperty {
    @GlobalProperty(name = "simulatorConfigurationFile", defaultValue = "")
    public static String SIMULATOR_CONFIGURATION_FILE;
    @GlobalProperty(name = "agentPort", defaultValue = "8080")
    public static int SIMULATOR_AGENT_PORT;
    @GlobalProperty(name = "agentRootPath", defaultValue = "/zstack")
    public static String SIMULATOR_AGENT_ROOT_PATH;
    @GlobalProperty(name = "redeployDB", defaultValue = "false")
    public static boolean REDEPLOY_DB;
}

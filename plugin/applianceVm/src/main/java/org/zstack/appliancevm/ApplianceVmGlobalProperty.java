package org.zstack.appliancevm;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 */
@GlobalPropertyDefinition
public class ApplianceVmGlobalProperty {
    @GlobalProperty(name = "ApplianceVm.noRollbackOnPostFailure", defaultValue = "false")
    public static boolean NO_ROLLBACK_ON_POST_FAILURE;
    @GlobalProperty(name = "ApplianceVm.connectVerbose", defaultValue = "false")
    public static boolean CONNECT_VERBOSE;
    @GlobalProperty(name="ApplianceVm.agentPackageName", defaultValue = "appliancevm-5.0.0.tar.gz")
    public static String AGENT_PACKAGE_NAME;
    @GlobalProperty(name="ApplianceVm.agentPort", defaultValue = "7759")
    public static int AGENT_PORT;
    @GlobalProperty(name="ApplianceVm.agentUrlScheme", defaultValue = "http")
    public static String AGENT_URL_SCHEME;
    @GlobalProperty(name="ApplianceVm.agentUrlRootPath", defaultValue = "")
    public static String AGENT_URL_ROOT_PATH;
}

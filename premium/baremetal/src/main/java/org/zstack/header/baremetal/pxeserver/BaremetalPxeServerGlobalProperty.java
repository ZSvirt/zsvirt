package org.zstack.header.baremetal.pxeserver;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

import java.util.List;

/**
 * Created by GuoYi on 2018-10-11.
 */
@GlobalPropertyDefinition
public class BaremetalPxeServerGlobalProperty {
    @GlobalProperty(name="BaremetalPxeServer.agentPackageName", defaultValue = "baremetalpxeserver-5.0.0.tar.gz")
    public static String AGENT_PACKAGE_NAME;
    @GlobalProperty(name="BaremetalPxeServer.agentPort", defaultValue = "7770")
    public static int AGENT_PORT;
    @GlobalProperty(name="BaremetalPxeServer.agentUrlScheme", defaultValue = "http")
    public static String AGENT_URL_SCHEME;
    @GlobalProperty(name="BaremetalPxeServer.agentUrlRootPath", defaultValue = "")
    public static String AGENT_URL_ROOT_PATH;
    @GlobalProperty(name="BaremetalPxeServer.pushgateway.port", defaultValue = "9093")
    public static String PUSHGATEWAY_PORT;
    @GlobalProperty(name="MN.network.", defaultValue = "")
    public static List<String> MN_NETWORKS;
}

package org.zstack.header.console;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:51 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ConsoleConstants {
    public static final String SERVICE_ID = "console";

    public static final String ACTION_CATEGORY = "console";

    public static final String MANAGEMENT_SERVER_CONSOLE_PROXY_BACKEND = "ManagementServerConsoleProxyBackend";

    public static final String MANAGEMENT_SERVER_CONSOLE_PROXY_TYPE = "ManagementServerConsoleProxy";

    public static final String CONSOLE_PROXY_ESTABLISH_PROXY_PATH = "/console/establish";
    public static final String CONSOLE_PROXY_CHECK_PROXY_PATH = "/console/check";
    public static final String CONSOLE_PROXY_DELETE_PROXY_PATH = "/console/delete";
    public static final String CONSOLE_PROXY_PING_PATH = "/console/ping";

    String VNC_IPTABLES_COMMENTS = "vnc.allow.port";

    String VNC_SCHEMA = "vnc";
    String HTTP_SCHEMA = "http";
    String WSS_SCHEMA = "wss";

    public static enum ConsoleGlobalConfig {
        ProxyIdleTimeout;

        public String getCategory() {
            return "Console";
        }
    }
}

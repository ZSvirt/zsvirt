package org.zstack.console;

import com.google.gson.annotations.SerializedName;
import org.zstack.core.ansible.SyncTimeRequestedDeployArguments;

public class ConsoleProxyDeployArguments extends SyncTimeRequestedDeployArguments {
    @SerializedName("pkg_consoleproxy")
    private final String packageName = ConsoleGlobalProperty.AGENT_PACKAGE_NAME;
    @SerializedName("http_console_proxy_port")
    private String httpConsoleProxyPort;

    @Override
    public String getPackageName() {
        return packageName;
    }

    public String getHttpConsoleProxyPort() {
        return httpConsoleProxyPort;
    }

    public void setHttpConsoleProxyPort(String httpConsoleProxyPort) {
        this.httpConsoleProxyPort = httpConsoleProxyPort;
    }
}

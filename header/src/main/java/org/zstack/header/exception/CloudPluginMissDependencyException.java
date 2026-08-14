package org.zstack.header.exception;

public class CloudPluginMissDependencyException extends CloudRuntimeException {
    private static final long serialVersionUID = SerialVersionUID.CloudPluginMissDependencyException;

    public CloudPluginMissDependencyException(String pluginName, String missDependencyName) {
        super(new StringBuilder().append("Plugin " + pluginName + " requires runtime dependency: " + missDependencyName)
                .append(". However, Zstack cannot find " + missDependencyName + " in all registered plugins.")
                .append(" Correct your components.xml to configure it").toString());
    }
}

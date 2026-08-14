package org.zstack.zsv.storage.entity;

import java.util.HashMap;
import java.util.Map;

public class CephPluginConnectionView {
    private String ip;
    private String pluginType;
    private Map<String, Object> pluginProperties = new HashMap<>();
    /**
     * not null if this ip belongs to a management node
     */
    private String managementNodeUuid;
    private String hostUuid;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPluginType() {
        return pluginType;
    }

    public void setPluginType(String pluginType) {
        this.pluginType = pluginType;
    }

    public Map<String, Object> getPluginProperties() {
        return pluginProperties;
    }

    public void setPluginProperties(Map<String, Object> pluginProperties) {
        this.pluginProperties = pluginProperties;
    }

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public static CephPluginConnectionView __example__() {
        CephPluginConnectionView view = new CephPluginConnectionView();
        view.setIp("127.0.0.1");
        view.setPluginType("ZStonePlugin");
        view.setManagementNodeUuid("ff0c85f246515803ba21effa46e5df00");
        return view;
    }
}

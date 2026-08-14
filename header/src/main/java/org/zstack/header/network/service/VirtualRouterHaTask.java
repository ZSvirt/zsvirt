package org.zstack.header.network.service;

public class VirtualRouterHaTask {
    private String taskName;
    private String originRouterUuid;
    private String peerRouterUuid;
    private String jsonData;

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getOriginRouterUuid() {
        return originRouterUuid;
    }

    public void setOriginRouterUuid(String originRouterUuid) {
        this.originRouterUuid = originRouterUuid;
    }

    public String getPeerRouterUuid() {
        return peerRouterUuid;
    }

    public void setPeerRouterUuid(String peerRouterUuid) {
        this.peerRouterUuid = peerRouterUuid;
    }

    public String getJsonData() {
        return jsonData;
    }

    public void setJsonData(String jsonData) {
        this.jsonData = jsonData;
    }
}

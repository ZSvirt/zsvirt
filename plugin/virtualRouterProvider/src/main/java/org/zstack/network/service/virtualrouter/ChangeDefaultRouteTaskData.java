package org.zstack.network.service.virtualrouter;

public class ChangeDefaultRouteTaskData {
    private String oldL3uuid;
    private String newL3uuid;

    public String getOldL3uuid() {
        return oldL3uuid;
    }

    public void setOldL3uuid(String oldL3uuid) {
        this.oldL3uuid = oldL3uuid;
    }

    public String getNewL3uuid() {
        return newL3uuid;
    }

    public void setNewL3uuid(String newL3uuid) {
        this.newL3uuid = newL3uuid;
    }
}

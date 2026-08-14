package org.zstack.zops;

public class ManagementNodeBaseInfo extends HostBaseInfo{
    private String uuid;
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public ManagementNodeBaseInfo(String uuid) {
        this.getTypes().add(HostType.ManagementNode);
        this.uuid = uuid;
    }
}

package org.zstack.storage.device.nvme;

import org.zstack.header.message.NeedReplyMessage;

public class ConnectNvmeServerMsg extends NeedReplyMessage {
    private String hostUuid;
    private String nvmeServerUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getNvmeServerUuid() {
        return nvmeServerUuid;
    }

    public void setNvmeServerUuid(String nvmeServerUuid) {
        this.nvmeServerUuid = nvmeServerUuid;
    }
}

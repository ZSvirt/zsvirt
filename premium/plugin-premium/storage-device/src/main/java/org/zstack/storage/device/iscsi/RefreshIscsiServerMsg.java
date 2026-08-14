package org.zstack.storage.device.iscsi;

import org.zstack.header.message.NeedReplyMessage;

public class RefreshIscsiServerMsg extends NeedReplyMessage {
    private String iscsiServerUuid;
    private String hostUuid;

    public String getIscsiServerUuid() {
        return iscsiServerUuid;
    }

    public void setIscsiServerUuid(String iscsiServerUuid) {
        this.iscsiServerUuid = iscsiServerUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}

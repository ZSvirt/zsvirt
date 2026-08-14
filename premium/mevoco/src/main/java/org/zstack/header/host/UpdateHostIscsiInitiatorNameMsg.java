package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class UpdateHostIscsiInitiatorNameMsg extends NeedReplyMessage implements HostMessage {
    private String uuid;
    private String iscsiInitiatorName;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getIscsiInitiatorName() {
        return iscsiInitiatorName;
    }

    public void setIscsiInitiatorName(String iscsiInitiatorName) {
        this.iscsiInitiatorName = iscsiInitiatorName;
    }

    @Override
    public String getHostUuid() {
        return uuid;
    }
}

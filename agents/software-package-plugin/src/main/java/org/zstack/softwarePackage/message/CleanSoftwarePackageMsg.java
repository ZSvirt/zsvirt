package org.zstack.softwarePackage.message;

import org.zstack.header.message.NeedReplyMessage;

public class CleanSoftwarePackageMsg extends NeedReplyMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
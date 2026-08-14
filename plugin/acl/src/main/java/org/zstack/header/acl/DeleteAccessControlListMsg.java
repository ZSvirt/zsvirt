package org.zstack.header.acl;

import org.zstack.header.message.DeletionMessage;

public class DeleteAccessControlListMsg extends DeletionMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
package org.zstack.header.vm.additions;

import org.zstack.header.message.DeletionMessage;

public class VmHostBackupFileDeletionMsg extends DeletionMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}

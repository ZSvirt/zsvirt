package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

public class GetBackupStorageManagerHostnameReply extends MessageReply {
    private String hostname;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}

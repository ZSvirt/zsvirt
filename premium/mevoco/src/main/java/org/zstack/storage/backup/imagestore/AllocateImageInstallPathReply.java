package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.MessageReply;

public class AllocateImageInstallPathReply extends MessageReply {
    private String name;
    private String installPath;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
